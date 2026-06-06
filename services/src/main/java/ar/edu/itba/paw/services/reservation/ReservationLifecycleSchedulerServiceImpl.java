package ar.edu.itba.paw.services.reservation;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.email.RiderCarReturnEmailPayload;
import ar.edu.itba.paw.models.email.RiderReviewInviteEmailPayload;
import ar.edu.itba.paw.policy.ReservationTimingPolicy;
import ar.edu.itba.paw.util.ReservationMailComposer;

import ar.edu.itba.paw.services.review.ReviewService;
/**
 * Scheduled lifecycle jobs (return reminders, return checkout notifications, rider review
 * invitations, automatic review-skipping) plus per-car analytics consumed by the owner car
 * detail dashboard. Kept apart from the rider-facing workflow so each file stays focused and
 * within the project's size budget.
 *
 * Architectural rule: this service no longer touches {@code ReservationDao} — row reads
 * and mutations are funneled through {@link ReservationService} (the sole DAO owner).
 */
@Service
public final class ReservationLifecycleSchedulerServiceImpl implements ReservationLifecycleSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationLifecycleSchedulerServiceImpl.class);

    private final ReservationService reservationService;
    private final ReservationTimingPolicy reservationTimingPolicy;
    private final ReservationPricingService pricingService;
    private final ReviewService reviewService;
    private final ReservationMailComposer mailComposer;

    @Autowired
    public ReservationLifecycleSchedulerServiceImpl(
            @Lazy final ReservationService reservationService,
            final ReservationTimingPolicy reservationTimingPolicy,
            final ReservationPricingService pricingService,
            @Lazy final ReviewService reviewService,
            final ReservationMailComposer mailComposer) {
        this.reservationService = reservationService;
        this.reservationTimingPolicy = reservationTimingPolicy;
        this.pricingService = pricingService;
        this.reviewService = reviewService;
        this.mailComposer = mailComposer;
    }

    // ---------------------------------------------------------------------------------------
    // Return reminder + checkout + review invite emails
    // ---------------------------------------------------------------------------------------

    @Override
    @Transactional
    public void dispatchReturnReminderEmails() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final int hours = reservationTimingPolicy.getReturnReminderHoursBeforeCheckout();
        final List<Reservation> candidates = reservationService.findReservationsForReturnReminderEmail(now, hours);
        LOGGER.atInfo().addArgument(candidates.size()).addArgument(hours)
                .log("Return reminder run: {} candidate reservation(s) (within {} h before checkout)");
        int queued = 0;
        for (final Reservation reservation : candidates) {
            final Optional<RiderCarReturnEmailPayload> payload = mailComposer.buildRiderCarReturnPayload(reservation);
            if (payload.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId())
                        .log("Skipping return reminder: missing data (reservation id={})");
                continue;
            }
            if (reservationService.claimReturnReminderEmailSent(reservation.getId()) == 0) {
                continue;
            }
            try {
                mailComposer.sendRiderReturnReminder(payload.get());
                queued++;
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId())
                        .log("Failed to queue return reminder email (reservation id={})");
            }
        }
        LOGGER.atInfo().addArgument(queued).log("Return reminder run: queued {} email(s)");
    }

    @Override
    @Transactional
    public void dispatchReturnCheckoutEmails() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final List<Reservation> candidates = reservationService.findReservationsForReturnCheckoutEmail(now);
        LOGGER.atInfo().addArgument(candidates.size()).log("Return checkout email run: {} candidate reservation(s)");
        int queued = 0;
        for (final Reservation reservation : candidates) {
            final Optional<RiderCarReturnEmailPayload> payload = mailComposer.buildRiderCarReturnPayload(reservation);
            if (payload.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId())
                        .log("Skipping return checkout mail: missing data (reservation id={})");
                continue;
            }
            if (reservationService.claimReturnCheckoutEmailSent(reservation.getId()) == 0) {
                continue;
            }
            try {
                mailComposer.sendRiderReturnCheckout(payload.get());
                queued++;
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId())
                        .log("Failed to queue return checkout email (reservation id={})");
            }
        }
        LOGGER.atInfo().addArgument(queued).log("Return checkout email run: queued {} email(s)");
    }

    @Override
    @Transactional
    public void dispatchRiderReviewInviteEmails() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final List<Reservation> candidates = reservationService.findReservationsForRiderReviewInviteEmail(now);
        LOGGER.atInfo().addArgument(candidates.size()).log("Rider review invite run: {} candidate reservation(s)");
        int queued = 0;
        for (final Reservation reservation : candidates) {
            final Optional<RiderReviewInviteEmailPayload> payload = mailComposer.buildRiderReviewInvitePayload(reservation);
            if (payload.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId())
                        .log("Skipping rider review invite: missing data (reservation id={})");
                continue;
            }
            if (reservationService.claimRiderReviewInviteEmailSent(reservation.getId()) == 0) {
                continue;
            }
            try {
                mailComposer.sendRiderReviewInvite(payload.get());
                queued++;
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId())
                        .log("Failed to queue rider review invite email (reservation id={})");
            }
        }
        LOGGER.atInfo().addArgument(queued).log("Rider review invite run: queued {} email(s)");
    }

    @Override
    @Transactional
    public void dispatchReviewAutoSkips() {
        final int days = reservationTimingPolicy.getReviewAutoSkipDays();
        if (days < 1) {
            LOGGER.atInfo().addArgument(days).log("Review auto-skip run skipped: feature disabled (days={})");
            return;
        }
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final OffsetDateTime endDateCutoff = now.minusDays(days);
        final OffsetDateTime carReturnedAtCutoff = now.minusDays(days);

        // Rider side: window starts at the rental endDate.
        final List<Reservation> riderCandidates =
                reservationService.findReservationsForRiderReviewAutoSkip(now, endDateCutoff);
        LOGGER.atInfo().addArgument(riderCandidates.size()).addArgument(days)
                .log("Review auto-skip (rider) run: {} candidate reservation(s) (window {} days)");
        int rDone = 0;
        for (final Reservation r : riderCandidates) {
            if (reviewService.hasRiderReview(r.getId())) {
                continue;
            }
            try {
                reviewService.submitRiderReviewOfOwner(r.getRiderId(), r.getId(), null, null);
                rDone++;
            } catch (final RiderReservationException e) {
                LOGGER.atWarn().setCause(e).addArgument(r.getId())
                        .log("Auto-skip rider review failed (reservation id={})");
            }
        }
        LOGGER.atInfo().addArgument(rDone).log("Review auto-skip (rider) run: closed {} review(s)");

        // Owner side: window starts at the moment the owner marked the car returned.
        final List<Reservation> ownerCandidates =
                reservationService.findReservationsForOwnerReviewAutoSkip(now, carReturnedAtCutoff);
        LOGGER.atInfo().addArgument(ownerCandidates.size()).addArgument(days)
                .log("Review auto-skip (owner) run: {} candidate reservation(s) (window {} days)");
        int oDone = 0;
        for (final Reservation r : ownerCandidates) {
            if (reviewService.hasOwnerReview(r.getId())) {
                continue;
            }
            final long ownerId = Optional.ofNullable(r.getCar())
                    .map(Car::getOwner)
                    .map(User::getId)
                    .orElse(0L);
            if (ownerId == 0L) {
                continue;
            }
            try {
                reviewService.submitOwnerReviewOfRider(ownerId, r.getId(), null, null);
                oDone++;
            } catch (final RiderReservationException e) {
                LOGGER.atWarn().setCause(e).addArgument(r.getId())
                        .log("Auto-skip owner review failed (reservation id={})");
            }
        }
        LOGGER.atInfo().addArgument(oDone).log("Review auto-skip (owner) run: closed {} review(s)");
    }

    // ---------------------------------------------------------------------------------------
    // Per-car analytics
    // ---------------------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countCarReservationsByStatus(final long ownerId, final long carId) {
        return mergeCancelledBuckets(reservationService.countCarReservationsByStatus(ownerId, carId));
    }

    /** Keeps a single {@code cancelled} counter for owner dashboards while persisting granular statuses. */
    private static Map<String, Long> mergeCancelledBuckets(final Map<String, Long> raw) {
        final Map<String, Long> out = new LinkedHashMap<>(raw);
        long merged = out.getOrDefault("cancelled", 0L);
        merged += out.getOrDefault("cancelled_by_rider", 0L);
        merged += out.getOrDefault("cancelled_by_owner", 0L);
        merged += out.getOrDefault("cancelled_due_to_missing_payment_proof", 0L);
        out.remove("cancelled_by_rider");
        out.remove("cancelled_by_owner");
        out.remove("cancelled_due_to_missing_payment_proof");
        out.put("cancelled", merged);
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCarTotalEarnings(final long ownerId, final long carId) {
        return reservationService.sumCarRevenueByStatuses(
                ownerId, carId, Arrays.asList("accepted", "started", "finished"));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCarPendingEarnings(final long ownerId, final long carId) {
        return reservationService.sumCarRevenueByStatuses(
                ownerId, carId, Arrays.asList("accepted", "started"));
    }

    @Override
    @Transactional(readOnly = true)
    public long getCarTotalDaysRented(final long ownerId, final long carId) {
        return reservationService.findCarFinishedReservations(ownerId, carId)
                .stream()
                .mapToLong(r -> pricingService.calculateBillableDays(r.getStartDate(), r.getEndDate()))
                .sum();
    }

    @Override
    @Transactional(readOnly = true)
    public long getCarReservationsThisMonth(final long ownerId, final long carId) {
        final YearMonth current = YearMonth.now(ZoneOffset.UTC);
        final OffsetDateTime monthStart = current.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        final OffsetDateTime nextMonthStart = current.plusMonths(1).atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        return reservationService.countCarReservationsCreatedBetween(ownerId, carId, monthStart, nextMonthStart);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OffsetDateTime> getCarNextReservationDate(final long ownerId, final long carId) {
        return reservationService.findCarNextActiveReservationDate(
                ownerId, carId, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
