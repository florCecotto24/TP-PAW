package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.ReservationCard;
import ar.edu.itba.paw.models.util.ReservationSearchCriteria;

/** Reservations, overlap checks, analytics, and mail-claim flags. */
public interface ReservationDao {

    /** Returns true if any blocking reservation for the car overlaps the given range. */
    boolean hasActiveOverlapByCar(long carId, OffsetDateTime startDate, OffsetDateTime endDate);

    /** Blocking reservations ({@code pending}, {@code accepted}, {@code started}) for a car. */
    List<Reservation> findBlockingByCarId(long carId);

    /**
     * Blocking reservations for {@code carId} whose date range intersects {@code [from, to]} (UTC,
     * exclusive end is normalised by the implementation). Used by the owner availability-edit flow to
     * decide whether withdrawing a set of days would conflict with already-active reservations.
     */
    List<Reservation> findBlockingByCarIdInRange(long carId, OffsetDateTime from, OffsetDateTime to);

    /**
     * Creates a reservation for a car. Sets {@code car_id} only.
     */
    Reservation createReservationForCar(
            long riderId,
            long carId,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Reservation.Status status,
            BigDecimal totalPrice,
            OffsetDateTime paymentProofDeadlineAt);

    List<Reservation> findPendingPaymentPastDeadline(OffsetDateTime now);

    int attachPaymentReceiptAndAccept(long reservationId, long riderId, long storedFileId);

    Optional<Reservation> getReservationById(long id);

    Optional<Reservation> getOwnerReservationById(long ownerId, long reservationId);

    Page<ReservationCard> getRiderReservationCards(ReservationSearchCriteria criteria);

    List<Reservation> getReminderReservations(final OffsetDateTime from, final OffsetDateTime to);

    Page<ReservationCard> getOwnerReservationCards(ReservationSearchCriteria criteria);

    int updateReservationStatus(long reservationId, String status);

    /** Owner car dashboard: paginated reservation cards for one car. */
    Page<ReservationCard> getCarReservationCards(long ownerId, long carId, int page, int pageSize, String statusFilter);

    /** Counts reservations per status bucket for the owner's car dashboard. */
    Map<String, Long> countCarReservationsByStatus(long ownerId, long carId);

    /** Sum of {@code total_price} for reservations in the given statuses, scoped to a car. */
    BigDecimal sumCarRevenueByStatuses(long ownerId, long carId, Collection<String> statuses);

    /** Count of reservations for a car created within {@code [from, until)}. */
    long countCarReservationsCreatedBetween(long ownerId, long carId, OffsetDateTime from, OffsetDateTime until);

    /** Earliest start_date of an accepted/started reservation on the car strictly after {@code after}. */
    Optional<OffsetDateTime> findCarNextActiveReservationDate(long ownerId, long carId, OffsetDateTime after);

    /** Finished reservations for the car (owner analytics). */
    List<Reservation> findCarFinishedReservations(long ownerId, long carId);

    /** Sets {@code car_returned}; transitions {@code status} to {@code finished} when the car is marked returned. */
    int markCarReturned(long reservationId, long ownerUserId);

    List<Reservation> findReservationsForReturnReminderEmail(OffsetDateTime now, int hoursBeforeCheckout);

    List<Reservation> findReservationsForReturnCheckoutEmail(OffsetDateTime now);

    List<Reservation> findReservationsForRiderReviewInviteEmail(OffsetDateTime now);

    /** Sets {@code return_reminder_email_sent} if still false; returns rows updated (0 or 1). */
    int claimReturnReminderEmailSent(long reservationId);

    /** Sets {@code return_checkout_email_sent} if still false; returns rows updated (0 or 1). */
    int claimReturnCheckoutEmailSent(long reservationId);

    /** Sets {@code rider_review_invite_email_sent} if still false; returns rows updated (0 or 1). */
    int claimRiderReviewInviteEmailSent(long reservationId);

    /**
     * Finds reservations with a payment proof deadline within 2 hours from now,
     * payment not yet approved, and email not yet sent.
     */
    List<Reservation> findReservationsWithDuePendingPaymentProof(OffsetDateTime now);

    /** Sets {@code pending_paymentproof_email_sent} if still false; returns rows updated (0 or 1). */
    int claimPendingPaymentProofEmailSent(long reservationId);

    /**
     * Participant cancellation: status plus optional refund obligation (confirmed reservation with prior payment).
     */
    int updateParticipantCancellationWithRefundMeta(
            long reservationId,
            String statusLower,
            boolean paymentRefundRequired,
            OffsetDateTime refundProofDeadlineAtOrNull);

    int attachRefundReceipt(long reservationId, long ownerUserId, long storedFileId);

    int updatePaymentRefundApproved(long reservationId, long riderUserId, boolean approved);

    List<Reservation> findReservationsWithDuePendingRefundProof(OffsetDateTime now);

    int claimPendingRefundEmailSent(long reservationId);

    /** All reservations paginated for admin view; ordered by creation date descending. */
    Page<ReservationCard> findAllReservationCards(int page, int pageSize);
}
