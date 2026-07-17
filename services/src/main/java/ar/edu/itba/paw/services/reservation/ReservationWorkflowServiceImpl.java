package ar.edu.itba.paw.services.reservation;


import static ar.edu.itba.paw.util.ReservationServiceSupport.isBlank;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationAccessDeniedException;
import ar.edu.itba.paw.exception.reservation.ReservationCancelNotAllowedException;
import ar.edu.itba.paw.exception.reservation.ReservationConflictException;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.exception.user.CBUNotFoundException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.util.time.WallDateTimeParsing;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.dto.reservation.BlockingReservationProjection;
import ar.edu.itba.paw.models.domain.reservation.ReservationParticipantRole;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.services.reservation.ReservationPricingService.ReservationPlan;
import ar.edu.itba.paw.util.ReservationMailComposer;

import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.user.UserReadinessService;
import ar.edu.itba.paw.services.user.UserService;
/**
 * Mutating reservation lifecycle: rider submissions, rider-driven period edits, participant
 * cancellations, the system-driven cancellation used by the payment-proof expiration job,
 * and the owner "car returned" toggle. All validation and pricing math is delegated to
 * {@link ReservationPricingService}; lookups to {@link ReservationQueryService}; emails to
 * {@link ReservationMailComposer}; the refund-obligation email that follows a confirmed
 * cancellation to {@link ReservationPaymentService}.
 *
 * <p>Architectural rule: this service no longer touches {@code ReservationDao} — row reads
 * and mutations are funneled through {@link ReservationService} (the sole DAO owner).</p>
 */
@Service
public class ReservationWorkflowServiceImpl implements ReservationWorkflowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationWorkflowServiceImpl.class);

    private final ReservationService reservationService;
    private final ReservationAvailabilityService reservationAvailabilityService;
    private final ReservationQueryService queryService;
    private final ReservationPricingService pricingService;
    private final UserService userService;
    private final UserReadinessService userReadinessService;
    private final CarService carService;
    private final ReservationMailComposer mailComposer;
    private final ReservationPaymentService reservationPaymentService;

    @Autowired
    public ReservationWorkflowServiceImpl(
            @Lazy final ReservationService reservationService,
            final ReservationAvailabilityService reservationAvailabilityService,
            final ReservationQueryService queryService,
            final ReservationPricingService pricingService,
            final UserService userService,
            final UserReadinessService userReadinessService,
            final CarService carService,
            final ReservationMailComposer mailComposer,
            @Lazy final ReservationPaymentService reservationPaymentService) {
        this.reservationService = reservationService;
        this.reservationAvailabilityService = reservationAvailabilityService;
        this.queryService = queryService;
        this.pricingService = pricingService;
        this.userService = userService;
        this.userReadinessService = userReadinessService;
        this.carService = carService;
        this.mailComposer = mailComposer;
        this.reservationPaymentService = reservationPaymentService;
    }

    // ---------------------------------------------------------------------------------------
    // Submit + edit
    // ---------------------------------------------------------------------------------------

    @Override
    @Transactional
    public Reservation submitRiderReservationByCar(
            final long riderId,
            final long carId,
            final Long availabilityId,
            final String fromDateTime,
            final String untilDateTime) {
        final Car car = carService.getCarById(carId)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND));
        // Defense in depth: the catalog already hides blocked-owner cars, but a direct POST should also fail.
        if (car.getOwner() != null && car.getOwner().isBlocked()) {
            throw new RiderReservationException(MessageKeys.RESERVATION_OWNER_BLOCKED);
        }
        // Only ACTIVE cars are bookable. Paused / admin-paused / lack-doc / unavailable / deactivated
        // cars may still expose OFFERED availability rows, but a direct POST must be rejected regardless.
        if (car.getStatus() != Car.Status.ACTIVE) {
            throw new RiderReservationException(MessageKeys.RESERVATION_CAR_NOT_ACTIVE);
        }
        final User rider = userService.getUserById(riderId)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.RESERVATION_RIDER_USER_NOT_FOUND));
        // Blocked riders may still authenticate (e.g. to finish in-progress trips) but must not
        // create new reservations. Existing reservations are left alone (see product rule N-34).
        if (rider.isBlocked()) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_BLOCKED);
        }
        final OffsetDateTime[] interval = parseAndOrderInterval(fromDateTime, untilDateTime);
        final OffsetDateTime startDate = interval[0];
        final OffsetDateTime endDate = interval[1];
        pricingService.validateRiderReservationPricingInterval(
                carId, availabilityId, startDate, endDate, car.getMinimumRentalDays());
        if (car.getOwner().getId() == rider.getId()) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_CANNOT_RESERVE_OWN_LISTING);
        }
        if (!userReadinessService.hasUploadedLicenseAndIdentity(rider)) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_DOCUMENTATION_REQUIRED);
        }
        final String cbu;
        try {
            cbu = userService.getUserCbu(car.getOwner().getId());
        } catch (final UserNotFoundException | CBUNotFoundException e) {
            LOGGER.atWarn().setCause(e).addArgument(car.getOwner().getId())
                    .log("Owner payment details unavailable for ownerId={}");
            throw new RiderReservationException(MessageKeys.RESERVATION_OWNER_PAYMENT_DETAILS_UNAVAILABLE);
        }
        final OffsetDateTime proofDeadline = OffsetDateTime.ofInstant(
                Instant.now().plus(pricingService.getConfiguredPaymentProofDeadlineHours(), ChronoUnit.HOURS),
                ZoneOffset.UTC);
        return createReservationForCar(
                rider.getId(), carId, car, startDate, endDate,
                Reservation.Status.PENDING, proofDeadline, cbu);
    }

    @Override
    @Transactional
    public Reservation editPendingReservationByRider(
            final long riderId,
            final long reservationId,
            final String fromDateTime,
            final String untilDateTime) {
        // Defense in depth: the security filter chain enforces that only the rider on a
        // still-unpaid PENDING reservation can reach this endpoint, but we re-check here so
        // a programmatic caller cannot bypass the same invariants.
        final Reservation reservation = queryService.getRiderReservationById(riderId, reservationId)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.RESERVATION_EDIT_NOT_ALLOWED));
        if (reservation.getStatus() != Reservation.Status.PENDING
                || reservation.getPaymentReceiptFileId().isPresent()) {
            throw new RiderReservationException(MessageKeys.RESERVATION_EDIT_NOT_ALLOWED);
        }
        final OffsetDateTime[] interval = parseAndOrderInterval(fromDateTime, untilDateTime);
        final OffsetDateTime startDate = interval[0];
        final OffsetDateTime endDate = interval[1];
        final long carId = reservation.getCarId();
        final Car car = carService.getCarById(carId)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND));
        // Moving or extending the reservation window re-introduces bookability, so a car that
        // became non-ACTIVE after the reservation was created can no longer be edited.
        if (car.getStatus() != Car.Status.ACTIVE) {
            throw new RiderReservationException(MessageKeys.RESERVATION_CAR_NOT_ACTIVE);
        }
        pricingService.validateRiderReservationPricingInterval(
                carId, null, startDate, endDate, car.getMinimumRentalDays());
        carService.lockForReservationWrite(carId);
        if (reservationService.hasActiveOverlapByCarExcluding(carId, startDate, endDate, reservationId)) {
            throw new ReservationConflictException(MessageKeys.RESERVATION_CONFLICT_OVERLAP);
        }
        final LocalDate firstBillableDay = startDate.atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDate();
        final LocalDate lastBillableDay = endDate.atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDate();
        final ReservationPlan plan = pricingService.planReservationByCar(carId, firstBillableDay, lastBillableDay)
                .filter(p -> p.total().signum() > 0)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.RESERVATION_TOTAL_PRICE_INVALID));

        final OffsetDateTime newDeadline = OffsetDateTime.ofInstant(
                Instant.now().plus(pricingService.getConfiguredPaymentProofDeadlineHours(), ChronoUnit.HOURS),
                ZoneOffset.UTC);
        final int updated = reservationService.updateRiderPendingReservationPeriod(
                reservationId, riderId, startDate, endDate, plan.total(), newDeadline);
        if (updated == 0) {
            throw new RiderReservationException(MessageKeys.RESERVATION_EDIT_NOT_ALLOWED);
        }
        // Replace the covering availability bridge so subsequent reads of the per-day plan reflect the
        // freshly chosen window (the rider may have moved the period onto different availability rows).
        reservationAvailabilityService.deleteCoveringAvailabilities(reservationId);
        reservationAvailabilityService.insertCoveringAvailabilities(reservationId, plan.coveringAvailabilityIds());

        final Reservation refreshed = queryService.getRiderReservationById(riderId, reservationId).orElse(reservation);
        mailComposer.sendReservationEditedEmail(refreshed, plan.firstDayAvailability());
        LOGGER.atInfo()
                .addArgument(reservationId).addArgument(riderId)
                .addArgument(startDate).addArgument(endDate)
                .log("Rider riderId={} edited reservation id={} new period [{} -> {}]");
        return refreshed;
    }

    /**
     * Shared input pre-check for submit and edit: rejects blank inputs, unparseable wall-time
     * strings, and end-before-start ordering. Returns the parsed UTC instants.
     */
    private static OffsetDateTime[] parseAndOrderInterval(final String fromDateTime, final String untilDateTime) {
        if (isBlank(fromDateTime) || isBlank(untilDateTime)) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_DATES_REQUIRED);
        }
        final OffsetDateTime startDate;
        final OffsetDateTime endDate;
        try {
            startDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc(fromDateTime);
            endDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc(untilDateTime);
        } catch (final DateTimeParseException e) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_DATES_INVALID_FORMAT);
        }
        if (!endDate.isAfter(startDate)) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_END_NOT_AFTER_START);
        }
        return new OffsetDateTime[] {startDate, endDate};
    }

    private Reservation createReservationForCar(
            final long riderId,
            final long carId,
            final Car car,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final Reservation.Status status,
            final OffsetDateTime paymentProofDeadlineAt,
            final String ownerCbu) {
        carService.lockForReservationWrite(carId);
        if (reservationService.hasActiveOverlapByCar(carId, startDate, endDate)) {
            throw new ReservationConflictException(MessageKeys.RESERVATION_CONFLICT_OVERLAP);
        }
        final LocalDate firstBillableDay = startDate.atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDate();
        final LocalDate lastBillableDay = endDate.atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDate();
        final ReservationPlan plan = pricingService.planReservationByCar(carId, firstBillableDay, lastBillableDay)
                .filter(p -> p.total().signum() > 0)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.RESERVATION_TOTAL_PRICE_INVALID));
        final Reservation reservation = reservationService.createReservationForCar(
                riderId, carId, startDate, endDate, status, plan.total(), paymentProofDeadlineAt);
        reservationAvailabilityService.insertCoveringAvailabilities(reservation.getId(), plan.coveringAvailabilityIds());
        mailComposer.sendReservationCreatedEmail(riderId, carId, reservation, car, plan.firstDayAvailability(), ownerCbu);
        LOGGER.atInfo()
                .addArgument(reservation.getId()).addArgument(carId).addArgument(riderId).addArgument(status)
                .log("Created car-based reservation id={} carId={} riderId={} status={}");
        return reservation;
    }

    // ---------------------------------------------------------------------------------------
    // Cancellations
    // ---------------------------------------------------------------------------------------

    @Override
    @Transactional
    public Optional<Reservation> cancelReservation(final long reservationId) {
        final Reservation.Status status = Reservation.Status.CANCELLED_DUE_TO_MISSING_PAYMENT_PROOF;
        reservationService.updateReservationStatus(reservationId, status.name().toLowerCase(Locale.ROOT));
        final Optional<Reservation> reservationOpt = reservationService.getReservationById(reservationId);
        reservationOpt.ifPresent(r -> {
            mailComposer.sendCancellationEmail(r, true);
            LOGGER.atInfo().addArgument(reservationId)
                    .log("Reservation id={} cancelled (missing payment proof)");
        });
        return reservationOpt;
    }

    @Override
    @Transactional
    public void cancelBlockingReservationsForAdminCarPause(final long carId) {
        final OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC);
        for (final BlockingReservationProjection projection :
                reservationService.findBlockingReservationsByCarId(carId)) {
            reservationService.getReservationById(projection.getId())
                    .ifPresent(reservation -> cancelReservationForAdminCarPause(reservation, nowUtc));
        }
    }

    private void cancelReservationForAdminCarPause(final Reservation r, final OffsetDateTime nowUtc) {
        final long reservationId = r.getId();
        switch (r.getStatus()) {
            case PENDING, ACCEPTED, STARTED -> cancelConfirmedForAdminPause(reservationId, r, nowUtc);
            default -> {
                // no-op
            }
        }
    }

    private void cancelConfirmedForAdminPause(
            final long reservationId,
            final Reservation r,
            final OffsetDateTime nowUtc) {
        final boolean refundRequired = r.getPaymentReceiptFileId().isPresent();
        final OffsetDateTime refundDeadline = refundRequired
                ? nowUtc.plusHours(pricingService.getConfiguredRefundProofDeadlineHours())
                : null;
        if (reservationService.applyAdminCarPauseCancellation(reservationId, refundRequired, refundDeadline) == 0) {
            return;
        }
        final Optional<Reservation> cancelled = reservationService.getReservationById(reservationId);
        cancelled.ifPresent(res -> {
            mailComposer.sendCancellationEmail(res, !refundRequired);
            if (refundRequired) {
                reservationPaymentService.sendOwnerRefundProofObligationEmail(res, false);
            }
            LOGGER.atInfo().addArgument(reservationId)
                    .log("Admin car pause cancelled reservation id={}");
        });
    }

    @Override
    @Transactional
    public Optional<Reservation> cancelReservationAsParticipant(final long userId, final long reservationId) {
        final Optional<Reservation> opt = reservationService.getReservationById(reservationId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        final Reservation r = opt.get();
        final boolean isRider = r.getRiderId() == userId;
        final boolean isOwner = Optional.ofNullable(r.getCar())
                .map(c -> c.getOwner().getId() == userId)
                .orElse(false);
        if (!isRider && !isOwner) {
            return Optional.empty();
        }
        final Reservation.Status cancelledStatus =
                isRider ? Reservation.Status.CANCELLED_BY_RIDER : Reservation.Status.CANCELLED_BY_OWNER;
        final OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC);
        return switch (r.getStatus()) {
            case PENDING -> cancelPendingAsParticipant(userId, reservationId, r, cancelledStatus);
            case ACCEPTED -> cancelAcceptedAsParticipant(userId, reservationId, r, cancelledStatus, nowUtc);
            default -> Optional.empty();
        };
    }

    @Override
    @Transactional
    public Reservation cancelReservationAsParticipantScoped(
            final long viewerUserId, final long reservationId, final ReservationParticipantRole viewerRole) {
        final Optional<Reservation> reservationOpt = viewerRole == null
                ? Optional.empty()
                : switch (viewerRole) {
                    case RIDER -> queryService.getRiderReservationById(viewerUserId, reservationId);
                    case OWNER -> queryService.getOwnerReservationById(viewerUserId, reservationId);
                };
        if (reservationOpt.isEmpty()) {
            throw ReservationAccessDeniedException.denied();
        }
        return cancelReservationAsParticipant(viewerUserId, reservationId)
                .orElseThrow(ReservationCancelNotAllowedException::notAllowed);
    }

    private Optional<Reservation> cancelPendingAsParticipant(
            final long userId,
            final long reservationId,
            final Reservation r,
            final Reservation.Status cancelledStatus) {
        if (r.getPaymentReceiptFileId().isPresent()) {
            return Optional.empty();
        }
        final int updated = reservationService.updateParticipantCancellationWithRefundMeta(
                reservationId, cancelledStatus.name().toLowerCase(Locale.ROOT), false, null);
        if (updated == 0) {
            return Optional.empty();
        }
        final Optional<Reservation> cancelled = reservationService.getReservationById(reservationId);
        cancelled.ifPresent(res -> {
            mailComposer.sendCancellationEmail(res, true);
            LOGGER.atInfo().addArgument(userId).addArgument(reservationId).addArgument(cancelledStatus)
                    .log("Participant userId={} cancelled reservation id={} ({})");
        });
        return cancelled;
    }

    private Optional<Reservation> cancelAcceptedAsParticipant(
            final long userId,
            final long reservationId,
            final Reservation r,
            final Reservation.Status cancelledStatus,
            final OffsetDateTime nowUtc) {
        if (!nowUtc.isBefore(r.getStartDate())) {
            return Optional.empty();
        }
        final boolean refundRequired = r.getPaymentReceiptFileId().isPresent();
        final OffsetDateTime refundDeadline = refundRequired
                ? nowUtc.plusHours(pricingService.getConfiguredRefundProofDeadlineHours())
                : null;
        final int updated = reservationService.updateParticipantCancellationWithRefundMeta(
                reservationId, cancelledStatus.name().toLowerCase(Locale.ROOT), refundRequired, refundDeadline);
        if (updated == 0) {
            return Optional.empty();
        }
        final Optional<Reservation> cancelled = reservationService.getReservationById(reservationId);
        cancelled.ifPresent(res -> {
            mailComposer.sendCancellationEmail(res, !refundRequired);
            if (refundRequired) {
                reservationPaymentService.sendOwnerRefundProofObligationEmail(res, false);
            }
            LOGGER.atInfo().addArgument(userId).addArgument(reservationId).addArgument(cancelledStatus)
                    .log("Participant userId={} cancelled reservation id={} ({})");
        });
        return cancelled;
    }

    @Override
    @Transactional
    public Reservation patchReservation(
            final long viewerUserId,
            final long reservationId,
            final Reservation.Status cancellationStatus,
            final Boolean carReturned,
            final String fromDateTimeWall,
            final String untilDateTimeWall) {
        if (cancellationStatus != null) {
            final ReservationParticipantRole role = switch (cancellationStatus) {
                case CANCELLED_BY_RIDER -> ReservationParticipantRole.RIDER;
                case CANCELLED_BY_OWNER -> ReservationParticipantRole.OWNER;
                default -> throw ReservationCancelNotAllowedException.notAllowed();
            };
            cancelReservationAsParticipantScoped(viewerUserId, reservationId, role);
        }
        if (Boolean.TRUE.equals(carReturned)) {
            markCarReturnedByOwner(viewerUserId, reservationId);
        }
        if (fromDateTimeWall != null || untilDateTimeWall != null) {
            final Reservation existing = reservationService.getReservationById(reservationId)
                    .orElseThrow(ReservationAccessDeniedException::denied);
            final String from = fromDateTimeWall != null
                    ? fromDateTimeWall
                    : WallDateTimeParsing.formatUtcAsClientWallDateTimeInput(existing.getStartDate());
            final String until = untilDateTimeWall != null
                    ? untilDateTimeWall
                    : WallDateTimeParsing.formatUtcAsClientWallDateTimeInput(existing.getEndDate());
            editPendingReservationByRider(viewerUserId, reservationId, from, until);
        }
        return reservationService.getReservationById(reservationId)
                .orElseThrow(ReservationAccessDeniedException::denied);
    }

    // ---------------------------------------------------------------------------------------
    // Mark car returned
    // ---------------------------------------------------------------------------------------

    @Override
    @Transactional
    public void markCarReturnedByOwner(final long ownerUserId, final long reservationId) {
        final Reservation r = queryService.getOwnerReservationById(ownerUserId, reservationId)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.RESERVATION_MARK_RETURNED_NOT_ALLOWED));
        if (!OffsetDateTime.now(ZoneOffset.UTC).isAfter(r.getEndDate())) {
            throw new RiderReservationException(MessageKeys.RESERVATION_MARK_RETURNED_NOT_ALLOWED);
        }
        // ACCEPTED/STARTED: flujo normal. FINISHED sin carReturned: permite destrabar
        // la reseña del owner (el submit exige carReturned).
        if (r.getStatus() != Reservation.Status.ACCEPTED
                && r.getStatus() != Reservation.Status.STARTED
                && r.getStatus() != Reservation.Status.FINISHED) {
            throw new RiderReservationException(MessageKeys.RESERVATION_MARK_RETURNED_NOT_ALLOWED);
        }
        if (r.isCarReturned()) {
            return;
        }
        final int updated = reservationService.markCarReturned(reservationId, ownerUserId);
        if (updated == 0) {
            throw new RiderReservationException(MessageKeys.RESERVATION_MARK_RETURNED_NOT_ALLOWED);
        }
        syncFinishedStatusWithCarReturned(ownerUserId, reservationId);
        LOGGER.atInfo().addArgument(ownerUserId).addArgument(reservationId)
                .log("Owner ownerUserId={} marked car returned for reservation id={}");
    }

    /**
     * Sets {@link Reservation.Status#FINISHED} when the car is marked returned; if status was
     * {@code finished} and the car is no longer returned, reverts to
     * {@link Reservation.Status#STARTED}.
     */
    private void syncFinishedStatusWithCarReturned(final long ownerUserId, final long reservationId) {
        final Optional<Reservation> freshOpt = queryService.getOwnerReservationById(ownerUserId, reservationId);
        if (freshOpt.isEmpty()) {
            return;
        }
        final Reservation fresh = freshOpt.get();
        final boolean carReturned = fresh.isCarReturned();
        if (carReturned
                && (fresh.getStatus() == Reservation.Status.ACCEPTED
                        || fresh.getStatus() == Reservation.Status.STARTED)) {
            reservationService.updateReservationStatus(
                    reservationId, Reservation.Status.FINISHED.name().toLowerCase(Locale.ROOT));
        } else if (!carReturned && fresh.getStatus() == Reservation.Status.FINISHED) {
            reservationService.updateReservationStatus(
                    reservationId, Reservation.Status.STARTED.name().toLowerCase(Locale.ROOT));
        }
    }
}
