package ar.edu.itba.paw.util;


import static ar.edu.itba.paw.util.ReservationServiceSupport.formatHandoverLocation;
import static ar.edu.itba.paw.util.ReservationServiceSupport.trimName;
import static ar.edu.itba.paw.util.ReservationServiceSupport.trimToNull;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.user.CBUNotFoundException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.email.reservation.OwnerBlockedEmailPayload;
import ar.edu.itba.paw.models.email.reservation.OwnerPaymentProofReceivedEmailPayload;
import ar.edu.itba.paw.models.email.reservation.OwnerRefundProofObligationEmailPayload;
import ar.edu.itba.paw.models.email.reservation.ReservationCancellationEmailPayload;
import ar.edu.itba.paw.models.email.reservation.ReservationMailPayload;
import ar.edu.itba.paw.models.email.reservation.RiderCarReturnEmailPayload;
import ar.edu.itba.paw.models.email.reservation.RiderRefundProofReceivedEmailPayload;
import ar.edu.itba.paw.models.email.reservation.RiderReviewInviteEmailPayload;
import ar.edu.itba.paw.models.util.time.WallDateTimeDisplayFormat;
import ar.edu.itba.paw.services.email.EmailService;
import ar.edu.itba.paw.services.user.UserLocaleService;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.util.format.MoneyFormat;

/**
 * Builds and dispatches every reservation-related email. Centralising this here keeps the
 * individual reservation services (workflow, payment, scheduler) under the project's
 * per-file size budget and makes the email side of the lifecycle easier to scan in one place.
 *
 * <p>Each public method swallows {@link RuntimeException} from the mail layer so a transient
 * mail failure never aborts the surrounding transaction. Interactive flows inject
 * {@link EmailService}, which is the {@code @Primary}
 * {@link ar.edu.itba.paw.services.email.TransactionalEmailServiceDecorator}: dispatches run in
 * {@code afterCommit}, so a rolled-back reservation write never leaves a confirmation/cancel mail.
 * CBU lookups that fail return early — the underlying confirmation/reminder is harmless if we
 * cannot embed the owner's account details on the rider's side.
 */
@Component
public final class ReservationMailComposer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationMailComposer.class);

    private final UserService userService;
    private final UserLocaleService userLocaleService;
    private final EmailService emailService;
    private final ReservationServiceSupport support;
    private final CarAvailabilityAddressFormatter addressFormatter;
    private final MoneyFormat moneyFormat;

    @Autowired
    public ReservationMailComposer(
            final UserService userService,
            final UserLocaleService userLocaleService,
            final EmailService emailService,
            final ReservationServiceSupport support,
            final CarAvailabilityAddressFormatter addressFormatter,
            final MoneyFormat moneyFormat) {
        this.userService = userService;
        this.userLocaleService = userLocaleService;
        this.emailService = emailService;
        this.support = support;
        this.addressFormatter = addressFormatter;
        this.moneyFormat = moneyFormat;
    }

    // ---------------------------------------------------------------------------------------
    // Confirmation / edit
    // ---------------------------------------------------------------------------------------

    /**
     * Brand-new car-based reservation confirmation. Re-fetches rider and car so the caller
     * does not need to wire users / vehicle metadata; the owner's CBU comes from upstream
     * because the workflow service already had to resolve it to validate the reservation.
     */
    public void sendReservationCreatedEmail(
            final long riderId,
            final long carId,
            final Reservation reservation,
            final Car car,
            final CarAvailability pickupAvailability,
            final String ownerCbu) {
        try {
            final Optional<User> riderOpt = userService.getUserById(riderId);
            if (riderOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(riderId).addArgument(reservation.getId())
                        .log("Skipping car reservation confirmation email: rider not found for riderId={} reservationId={}");
                return;
            }
            final User carOwner = car == null ? null : car.getOwner();
            if (carOwner == null) {
                LOGGER.atWarn().addArgument(carId).addArgument(reservation.getId())
                        .log("Skipping car reservation confirmation email: car owner not found for carId={} reservationId={}");
                return;
            }
            final User rider = riderOpt.get();
            final String vehicleLabel = car.getBrand() + " " + car.getModel();
            final String handoverLocation = formatHandoverLocation(pickupAvailability);
            final ReservationMailPayload payload = ReservationMailPayload.builder()
                    .recipientEmail(rider.getEmail())
                    .riderFullName(rider.getForename() + " " + rider.getSurname())
                    .reservationId(reservation.getId())
                    .carId(carId)
                    .vehicleLabel(vehicleLabel)
                    .startDate(reservation.getStartDate())
                    .endDate(reservation.getEndDate())
                    .riderHandoverLocation(handoverLocation)
                    .ownerHandoverLocation(handoverLocation)
                    .ownerFullName(carOwner.getForename() + " " + carOwner.getSurname())
                    .ownerEmail(carOwner.getEmail())
                    .reservationTotal(moneyFormat.format(reservation.getTotalPrice()))
                    .riderMailLocale(userLocaleService.resolveMailLocaleFor(rider))
                    .ownerMailLocale(userLocaleService.resolveMailLocaleFor(carOwner))
                    .ownerCbu(ownerCbu)
                    .build();
            LOGGER.atInfo().addArgument(rider.getEmail()).addArgument(reservation.getId())
                    .log("Queueing car-based reservation confirmation email to {} for reservation id={}");
            emailService.sendReservationConfirmationEmail(payload);
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(reservation.getId())
                    .log("Could not enqueue car-based reservation confirmation email for reservation id={}");
        }
    }

    /**
     * Rider-driven edit of an unpaid pending reservation. We re-use the confirmation copy on
     * purpose: the rider just chose new dates so they need the same payment-on-hold message
     * and CBU line as a brand-new pending reservation would receive.
     */
    public void sendReservationEditedEmail(final Reservation reservation, final CarAvailability pickupAvailability) {
        try {
            final Optional<User> riderOpt = resolveRider(reservation);
            final Optional<User> ownerOpt = support.resolveOwnerFromReservation(reservation);
            if (riderOpt.isEmpty() || ownerOpt.isEmpty()) {
                return;
            }
            final User rider = riderOpt.get();
            final User owner = ownerOpt.get();
            final String vehicleLabel = support.resolveVehicleLabelFromReservation(reservation);
            final String handoverLocation = formatHandoverLocation(pickupAvailability);
            final String ownerCbu;
            try {
                ownerCbu = userService.getUserCbu(owner.getId());
            } catch (final UserNotFoundException | CBUNotFoundException e) {
                LOGGER.atDebug()
                        .setMessage("Skipping rider-edit confirmation email: owner CBU unavailable reservationId={} ownerId={}")
                        .addArgument(reservation.getId())
                        .addArgument(owner.getId())
                        .setCause(e)
                        .log();
                return;
            }
            final ReservationMailPayload payload = ReservationMailPayload.builder()
                    .recipientEmail(rider.getEmail())
                    .riderFullName(rider.getForename() + " " + rider.getSurname())
                    .reservationId(reservation.getId())
                    .carId(reservation.getCarId())
                    .vehicleLabel(vehicleLabel)
                    .startDate(reservation.getStartDate())
                    .endDate(reservation.getEndDate())
                    .riderHandoverLocation(handoverLocation)
                    .ownerHandoverLocation(handoverLocation)
                    .ownerFullName(owner.getForename() + " " + owner.getSurname())
                    .ownerEmail(owner.getEmail())
                    .reservationTotal(moneyFormat.format(reservation.getTotalPrice()))
                    .riderMailLocale(userLocaleService.resolveMailLocaleFor(rider))
                    .ownerMailLocale(userLocaleService.resolveMailLocaleFor(owner))
                    .ownerCbu(ownerCbu)
                    .build();
            emailService.sendReservationConfirmationEmail(payload);
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(reservation.getId())
                    .log("Could not enqueue rider-edit confirmation email for reservation id={}");
        }
    }

    // ---------------------------------------------------------------------------------------
    // Cancellation
    // ---------------------------------------------------------------------------------------

    public void sendCancellationEmail(final Reservation reservation, final boolean notifyOwnerCancellation) {
        try {
            final long riderId = reservation.getRiderId();
            final Optional<User> riderOpt = resolveRider(reservation);
            final Optional<CarAvailability> availabilityOpt = support.resolveAvailabilityForReservation(reservation);
            final Optional<User> listingOwnerOpt = support.resolveOwnerFromReservation(reservation);
            if (riderOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(riderId).addArgument(reservation.getId())
                        .log("Skipping reservation cancellation email: user not found for riderId={} reservationId={}");
                return;
            }
            if (listingOwnerOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId())
                        .log("Skipping reservation cancellation email: owner not found for reservationId={}");
                return;
            }
            final User rider = riderOpt.get();
            final User listingOwner = listingOwnerOpt.get();
            final String vehicleLabel = support.resolveVehicleLabelFromReservation(reservation);
            final String riderFullName = rider.getForename() + " " + rider.getSurname();
            final String riderLoc = availabilityOpt
                    .map(a -> trimToNull(addressFormatter.formatRiderReservationHandoverSummary(a, reservation)))
                    .orElse(null);
            final String ownerLoc = availabilityOpt
                    .map(a -> trimToNull(addressFormatter.formatOwnerReservationHandoverSummary(a)))
                    .orElse(null);
            final ReservationMailPayload mail = ReservationMailPayload.builder()
                    .recipientEmail(rider.getEmail())
                    .riderFullName(riderFullName)
                    .reservationId(reservation.getId())
                    .carId(reservation.getCarId())
                    .vehicleLabel(vehicleLabel)
                    .startDate(reservation.getStartDate())
                    .endDate(reservation.getEndDate())
                    .riderHandoverLocation(riderLoc)
                    .ownerHandoverLocation(ownerLoc)
                    .ownerFullName(listingOwner.getForename() + " " + listingOwner.getSurname())
                    .ownerEmail(listingOwner.getEmail())
                    .reservationTotal(moneyFormat.format(reservation.getTotalPrice()))
                    .riderMailLocale(userLocaleService.resolveMailLocaleFor(rider))
                    .ownerMailLocale(userLocaleService.resolveMailLocaleFor(listingOwner))
                    .build();
            final ReservationCancellationEmailPayload cancelPayload = ReservationCancellationEmailPayload.builder()
                    .mail(mail)
                    .cancellationStatus(reservation.getStatus())
                    .notifyOwnerCancellation(notifyOwnerCancellation)
                    .build();
            LOGGER.atInfo().addArgument(rider.getEmail()).addArgument(reservation.getId())
                    .log("Queueing reservation cancellation email to {} for reservation id={}");
            emailService.sendReservationCancellationEmail(cancelPayload);
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(reservation.getId())
                    .log("Could not enqueue reservation cancellation email for reservation id={}");
        }
    }

    // ---------------------------------------------------------------------------------------
    // Payment proof: rider-side confirmation + owner-side notification
    // ---------------------------------------------------------------------------------------

    public void sendRiderReservationConfirmedAfterPaymentProof(final long riderId, final Reservation reservation) {
        try {
            final Optional<User> riderOpt = userService.getUserById(riderId);
            final Optional<CarAvailability> availabilityOpt = support.resolveAvailabilityForReservation(reservation);
            final Optional<User> listingOwnerOpt = support.resolveOwnerFromReservation(reservation);
            if (riderOpt.isEmpty() || listingOwnerOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId()).log(
                        "Skipping rider confirmed-after-proof email: missing rider or owner (reservation id={})");
                return;
            }
            final User rider = riderOpt.get();
            final User listingOwner = listingOwnerOpt.get();
            final String vehicleLabel = support.resolveVehicleLabelFromReservation(reservation);
            final String riderLoc = availabilityOpt
                    .map(a -> trimToNull(addressFormatter.formatRiderReservationHandoverSummary(a, reservation)))
                    .orElse(null);
            final String ownerLoc = availabilityOpt
                    .map(a -> trimToNull(addressFormatter.formatOwnerReservationHandoverSummary(a)))
                    .orElse(null);
            final ReservationMailPayload payload = ReservationMailPayload.builder()
                    .recipientEmail(rider.getEmail())
                    .riderFullName(rider.getForename() + " " + rider.getSurname())
                    .reservationId(reservation.getId())
                    .carId(reservation.getCarId())
                    .vehicleLabel(vehicleLabel)
                    .startDate(reservation.getStartDate())
                    .endDate(reservation.getEndDate())
                    .riderHandoverLocation(riderLoc)
                    .ownerHandoverLocation(ownerLoc)
                    .ownerFullName(listingOwner.getForename() + " " + listingOwner.getSurname())
                    .ownerEmail(listingOwner.getEmail())
                    .reservationTotal(moneyFormat.format(reservation.getTotalPrice()))
                    .riderMailLocale(userLocaleService.resolveMailLocaleFor(rider))
                    .ownerMailLocale(userLocaleService.resolveMailLocaleFor(listingOwner))
                    .build();
            LOGGER.atInfo().addArgument(rider.getEmail()).addArgument(reservation.getId())
                    .log("Queueing rider reservation confirmed-after-proof email to {} for reservation id={}");
            emailService.sendRiderReservationConfirmedAfterPaymentProof(payload);
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(reservation.getId())
                    .log("Could not enqueue rider confirmed-after-proof email for reservation id={}");
        }
    }

    public void sendOwnerPaymentProofReceived(final long reservationId, final long riderId, final Reservation reservation) {
        try {
            final Optional<User> ownerOpt = support.resolveOwnerFromReservation(reservation);
            final Optional<User> riderOpt = userService.getUserById(riderId);
            if (ownerOpt.isEmpty() || riderOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(reservationId)
                        .log("Skipping owner payment-proof email: missing owner or rider (reservation id={})");
                return;
            }
            final User owner = ownerOpt.get();
            final User rider = riderOpt.get();
            final OwnerPaymentProofReceivedEmailPayload mailPayload = OwnerPaymentProofReceivedEmailPayload.builder()
                    .messageLocale(userLocaleService.resolveMailLocaleFor(owner))
                    .recipientEmail(owner.getEmail())
                    .ownerFullName(owner.getForename() + " " + owner.getSurname())
                    .riderFullName(rider.getForename() + " " + rider.getSurname())
                    .riderEmail(rider.getEmail())
                    .reservationTotal(moneyFormat.format(reservation.getTotalPrice()))
                    .vehicleLabel(support.resolveVehicleLabelFromReservation(reservation))
                    .reservationId(reservationId)
                    .startDate(reservation.getStartDate())
                    .endDate(reservation.getEndDate())
                    .build();
            LOGGER.atInfo().addArgument(owner.getEmail()).addArgument(reservationId)
                    .log("Queueing owner payment-proof email to {} (reservation id={})");
            emailService.sendOwnerPaymentProofReceivedEmail(mailPayload);
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(reservationId)
                    .log("Could not enqueue owner payment-proof email for reservation id={}");
        }
    }

    // ---------------------------------------------------------------------------------------
    // Refund proof: rider-side notification + owner-side obligation / reminder
    // ---------------------------------------------------------------------------------------

    public void sendRiderRefundProofReceived(final long reservationId, final Reservation reservation) {
        try {
            final Optional<User> ownerOpt = support.resolveOwnerFromReservation(reservation);
            final Optional<User> riderOpt = resolveRider(reservation);
            if (ownerOpt.isEmpty() || riderOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(reservationId)
                        .log("Skipping rider refund-proof email: missing owner or rider (reservation id={})");
                return;
            }
            final User owner = ownerOpt.get();
            final User rider = riderOpt.get();
            final RiderRefundProofReceivedEmailPayload mailPayload = RiderRefundProofReceivedEmailPayload.builder()
                    .messageLocale(userLocaleService.resolveMailLocaleFor(rider))
                    .recipientEmail(rider.getEmail())
                    .riderFullName(rider.getForename() + " " + rider.getSurname())
                    .ownerFullName(owner.getForename() + " " + owner.getSurname())
                    .ownerEmail(owner.getEmail())
                    .reservationTotal(moneyFormat.format(reservation.getTotalPrice()))
                    .vehicleLabel(support.resolveVehicleLabelFromReservation(reservation))
                    .reservationId(reservationId)
                    .startDate(reservation.getStartDate())
                    .endDate(reservation.getEndDate())
                    .build();
            LOGGER.atInfo().addArgument(rider.getEmail()).addArgument(reservationId)
                    .log("Queueing rider refund-proof email to {} (reservation id={})");
            emailService.sendRiderRefundProofReceivedEmail(mailPayload);
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(reservationId)
                    .log("Could not enqueue rider refund-proof email for reservation id={}");
        }
    }

    /**
     * "You must upload a refund receipt within X" email. Used both as a one-shot obligation
     * notice (right after the confirmed cancellation) and as a recurring reminder from the
     * due-refund-proof scheduler — {@code dueReminder} toggles the copy variant.
     */
    public void sendOwnerRefundProofObligation(final Reservation reservation, final boolean dueReminder) {
        try {
            if (!reservation.isPaymentRefundRequired()) {
                return;
            }
            final Optional<OffsetDateTime> deadlineOpt = reservation.getRefundProofDeadlineAt();
            if (deadlineOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId())
                        .log("Skipping owner refund-proof email: no deadline (reservation id={})");
                return;
            }
            final Optional<User> ownerOpt = support.resolveOwnerFromReservation(reservation);
            if (ownerOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId())
                        .log("Skipping owner refund-proof email: owner not found (reservation id={})");
                return;
            }
            final User owner = ownerOpt.get();
            final OwnerRefundProofObligationEmailPayload mailPayload = OwnerRefundProofObligationEmailPayload.builder()
                    .messageLocale(userLocaleService.resolveMailLocaleFor(owner))
                    .recipientEmail(owner.getEmail())
                    .ownerFullName(owner.getForename() + " " + owner.getSurname())
                    .vehicleLabel(support.resolveVehicleLabelFromReservation(reservation))
                    .reservationId(reservation.getId())
                    .startDate(reservation.getStartDate())
                    .endDate(reservation.getEndDate())
                    .reservationTotal(moneyFormat.format(reservation.getTotalPrice()))
                    .refundProofDeadlineAt(deadlineOpt.get())
                    .dueReminder(dueReminder)
                    .build();
            LOGGER.atInfo().addArgument(owner.getEmail()).addArgument(reservation.getId())
                    .log("Queueing owner refund-proof obligation email to {} (reservation id={})");
            emailService.sendOwnerRefundProofObligationEmail(mailPayload);
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(reservation.getId())
                    .log("Could not enqueue owner refund-proof obligation email for reservation id={}");
        }
    }

    public void sendOwnerBlockedEmail(final User owner, final List<Reservation> overdueReservations) {
        try {
            final List<OwnerBlockedEmailPayload.OverdueRefundReservation> rows = new ArrayList<>();
            for (final Reservation r : overdueReservations) {
                final OffsetDateTime deadline = r.getRefundProofDeadlineAt().orElse(null);
                if (deadline == null) {
                    continue;
                }
                rows.add(new OwnerBlockedEmailPayload.OverdueRefundReservation(
                        r.getId(),
                        support.resolveVehicleLabelFromReservation(r),
                        deadline,
                        moneyFormat.format(r.getTotalPrice())));
            }
            if (rows.isEmpty()) {
                return;
            }
            final OwnerBlockedEmailPayload payload = OwnerBlockedEmailPayload.builder()
                    .messageLocale(userLocaleService.resolveMailLocaleFor(owner))
                    .recipientEmail(owner.getEmail())
                    .ownerFullName(owner.getForename() + " " + owner.getSurname())
                    .overdueReservations(rows)
                    .build();
            emailService.sendOwnerBlockedEmail(payload);
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(owner.getId())
                    .log("Could not enqueue owner-blocked email for ownerId={}");
        }
    }

    // ---------------------------------------------------------------------------------------
    // Due-payment-proof reminder (rider)
    // ---------------------------------------------------------------------------------------

    public Optional<ReservationMailPayload> buildDuePaymentProofReminder(final Reservation reservation) {
        final Optional<User> riderOpt = resolveRider(reservation);
        final Optional<User> ownerOpt = support.resolveOwnerFromReservation(reservation);
        if (riderOpt.isEmpty() || ownerOpt.isEmpty()) {
            return Optional.empty();
        }
        final User rider = riderOpt.get();
        final User owner = ownerOpt.get();
        final String vehicleLabel = support.resolveVehicleLabelFromReservation(reservation);
        final String ownerCbu;
        try {
            ownerCbu = userService.getUserCbu(owner.getId());
        } catch (final UserNotFoundException | CBUNotFoundException e) {
            LOGGER.atDebug()
                    .setCause(e)
                    .addArgument(reservation.getId())
                    .addArgument(owner.getId())
                    .log("Skipping due payment proof reminder: owner CBU unavailable (reservation id={}, owner id={})");
            return Optional.empty();
        }
        final Locale riderLocale = userLocaleService.resolveMailLocaleFor(rider);
        return Optional.of(ReservationMailPayload.builder()
                .recipientEmail(rider.getEmail())
                .riderFullName(rider.getForename() + " " + rider.getSurname())
                .reservationId(reservation.getId())
                .carId(reservation.getCarId())
                .vehicleLabel(vehicleLabel)
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .ownerFullName(owner.getForename() + " " + owner.getSurname())
                .ownerEmail(owner.getEmail())
                .reservationTotal(moneyFormat.format(reservation.getTotalPrice()))
                .ownerCbu(ownerCbu)
                .riderMailLocale(riderLocale)
                .ownerMailLocale(userLocaleService.resolveMailLocaleFor(owner))
                .build());
    }

    public void sendDuePaymentProofReminder(final ReservationMailPayload payload) {
        emailService.sendRiderDuePaymentProofEmail(payload);
    }

    // ---------------------------------------------------------------------------------------
    // Return reminder + return checkout + review invite (scheduler-driven)
    // ---------------------------------------------------------------------------------------

    /**
     * Day-before-pickup reminder. Rider, car and owner must come pre-fetched on the reservation
     * (see {@code getReminderReservations}); the pickup snapshot is batch-resolved by the caller,
     * so building the payload issues no per-row queries.
     */
    public Optional<ReservationMailPayload> buildReservationReminderPayload(
            final Reservation reservation, final CarAvailability pickupSnapshot) {
        final User rider = reservation.getRider();
        final Car car = reservation.getCar();
        if (rider == null || car == null || car.getOwner() == null) {
            return Optional.empty();
        }
        final User listingOwner = car.getOwner();
        final String riderLocation = pickupSnapshot == null
                ? null
                : addressFormatter.formatRiderReservationHandoverSummary(pickupSnapshot, reservation);
        final String ownerLocation = pickupSnapshot == null
                ? null
                : addressFormatter.formatOwnerReservationHandoverSummary(pickupSnapshot);
        return Optional.of(ReservationMailPayload.builder()
                .recipientEmail(rider.getEmail())
                .riderFullName(trimName(rider.getForename(), rider.getSurname()))
                .reservationId(reservation.getId())
                .carId(car.getId())
                .vehicleLabel(support.resolveVehicleLabelFromReservation(reservation))
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .riderHandoverLocation(riderLocation)
                .ownerHandoverLocation(ownerLocation)
                .ownerFullName(trimName(listingOwner.getForename(), listingOwner.getSurname()))
                .ownerEmail(listingOwner.getEmail())
                .reservationTotal(moneyFormat.format(reservation.getTotalPrice()))
                .riderMailLocale(userLocaleService.resolveMailLocaleFor(rider))
                .ownerMailLocale(userLocaleService.resolveMailLocaleFor(listingOwner))
                .build());
    }

    public void sendReservationReminder(final ReservationMailPayload payload) {
        emailService.sendReservationReminderEmail(payload);
    }

    public Optional<RiderCarReturnEmailPayload> buildRiderCarReturnPayload(final Reservation reservation) {
        final Optional<User> riderOpt = resolveRider(reservation);
        final Optional<CarAvailability> availabilityOpt = support.resolveAvailabilityForReservation(reservation);
        final Optional<User> ownerOpt = support.resolveOwnerFromReservation(reservation);
        if (riderOpt.isEmpty() || ownerOpt.isEmpty()) {
            return Optional.empty();
        }
        final User rider = riderOpt.get();
        final User owner = ownerOpt.get();
        final Locale locale = userLocaleService.resolveMailLocaleFor(rider);
        final String checkout = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(reservation.getEndDate(), locale);
        final String returnLine = availabilityOpt
                .map(a -> addressFormatter.formatPickupForReservationView(a, reservation, false))
                .orElse(null);
        final String path = "/my-reservations/" + reservation.getId();
        final String vehicleLabel = support.resolveVehicleLabelFromReservation(reservation);
        return Optional.of(RiderCarReturnEmailPayload.builder()
                .messageLocale(locale)
                .recipientEmail(rider.getEmail())
                .riderFullName(trimName(rider.getForename(), rider.getSurname()))
                .vehicleLabel(vehicleLabel)
                .ownerEmail(owner.getEmail())
                .checkoutFormatted(checkout)
                .returnLocationLine(returnLine)
                .reservationDetailPath(path)
                .build());
    }

    public void sendRiderReturnReminder(final RiderCarReturnEmailPayload payload) {
        emailService.sendRiderReturnReminderEmail(payload);
    }

    public void sendRiderReturnCheckout(final RiderCarReturnEmailPayload payload) {
        emailService.sendRiderReturnCheckoutEmail(payload);
    }

    public Optional<RiderReviewInviteEmailPayload> buildRiderReviewInvitePayload(final Reservation reservation) {
        final Optional<User> riderOpt = resolveRider(reservation);
        if (riderOpt.isEmpty()) {
            return Optional.empty();
        }
        final User rider = riderOpt.get();
        final String vehicleLabel = support.resolveVehicleLabelFromReservation(reservation);
        final Locale locale = userLocaleService.resolveMailLocaleFor(rider);
        /* Deep link must stay consistent with GET /my-reservations/{id} (default role=rider) + #rider-review-owner. */
        final String path = "/my-reservations/" + reservation.getId() + "?role=rider#rider-review-owner";
        return Optional.of(RiderReviewInviteEmailPayload.builder()
                .messageLocale(locale)
                .recipientEmail(rider.getEmail())
                .riderFullName(trimName(rider.getForename(), rider.getSurname()))
                .vehicleLabel(vehicleLabel)
                .reviewSectionPath(path)
                .build());
    }

    public void sendRiderReviewInvite(final RiderReviewInviteEmailPayload payload) {
        emailService.sendRiderReviewInviteEmail(payload);
    }

    /**
     * Prefer the already-hydrated rider association when present; otherwise load by id.
     * Call sites that receive an explicit {@code riderId} keep using {@code getUserById}
     * because the reservation graph may not include the rider.
     *
     * {@code getRider()} on a mandatory {@code @ManyToOne} returns a proxy even when
     * uninitialized — gate on JPA {@code PersistenceUtil.isLoaded} so mail jobs without FETCH
     * fall back to {@code getUserById} instead of tripping {@code LazyInitializationException}.
     */
    private Optional<User> resolveRider(final Reservation reservation) {
        final User rider = reservation.getRider();
        if (rider != null && javax.persistence.Persistence.getPersistenceUtil().isLoaded(rider)) {
            return Optional.of(rider);
        }
        return userService.getUserById(reservation.getRiderId());
    }
}
