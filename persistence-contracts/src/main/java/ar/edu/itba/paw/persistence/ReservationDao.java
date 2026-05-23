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

/** Reservations, overlap checks, listing analytics, and mail-claim flags. */
public interface ReservationDao {

    boolean hasActiveOverlap(long listingId, OffsetDateTime startDate, OffsetDateTime endDate);

    /** Like {@link #hasActiveOverlap(long, OffsetDateTime, OffsetDateTime)} but resolves by {@code car_id}. */
    boolean hasActiveOverlapByCar(long carId, OffsetDateTime startDate, OffsetDateTime endDate);

    List<Reservation> findBlockingByListingId(long listingId);

    List<Reservation> findBlockingByListingIds(Collection<Long> listingIds);

    /** Like {@link #findBlockingByListingId} but resolves by {@code car_id}. */
    List<Reservation> findBlockingByCarId(long carId);

    /**
     * Blocking reservations for {@code listingId} whose date range intersects {@code [from, to]} (UTC,
     * exclusive end is normalised by the implementation). Used by the owner availability-edit flow to
     * decide whether withdrawing a set of days would conflict with already-active reservations.
     */
    List<Reservation> findBlockingByListingIdInRange(long listingId, OffsetDateTime from, OffsetDateTime to);

    /** Like {@link #findBlockingByListingIdInRange} but resolves by {@code car_id}. */
    List<Reservation> findBlockingByCarIdInRange(long carId, OffsetDateTime from, OffsetDateTime to);

    Reservation createReservation(
            long riderId,
            long listingId,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Reservation.Status status,
            BigDecimal totalPrice,
            OffsetDateTime paymentProofDeadlineAt);

    /**
     * Creates a reservation directly for a car (no listing reference). Sets {@code car_id}; {@code listing_id}
     * remains null. Used by Phase 7b+ creation flow.
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

    /** Sets {@code payment_approved} only; lifecycle status is owned by the service layer. */
    int updatePaymentApproved(long reservationId, long ownerUserId, boolean approved);

    Optional<Reservation> getReservationById(long id);

    Optional<Reservation> getOwnerReservationById(long ownerId, long reservationId);

    Page<ReservationCard> getRiderReservationCards(ReservationSearchCriteria criteria);

    List<Reservation> getReminderReservations(final OffsetDateTime from, final OffsetDateTime to);

    Page<ReservationCard> getOwnerReservationCards(ReservationSearchCriteria criteria);

    int updateReservationStatus(long reservationId, String status);

    List<Reservation> getListingActiveReservations(long listingId);

    Page<ReservationCard> getListingReservationCards(long ownerId, long listingId, int page, int pageSize, String statusFilter);

    Page<ReservationCard> getCarReservationCards(long ownerId, long carId, int page, int pageSize, String statusFilter);

    Map<String, Long> countListingReservationsByStatus(long ownerId, long listingId);

    Map<String, Long> countCarReservationsByStatus(long ownerId, long carId);

    BigDecimal sumListingRevenueByStatuses(long ownerId, long listingId, Collection<String> statuses);

    long countListingReservationsCreatedBetween(long ownerId, long listingId, OffsetDateTime from, OffsetDateTime until);

    Optional<OffsetDateTime> findListingNextActiveReservationDate(long ownerId, long listingId, OffsetDateTime after);

    List<Reservation> findListingFinishedReservations(long ownerId, long listingId);

    /** Car-centric variant of {@link #sumListingRevenueByStatuses}. */
    BigDecimal sumCarRevenueByStatuses(long ownerId, long carId, Collection<String> statuses);

    /** Car-centric variant of {@link #countListingReservationsCreatedBetween}. */
    long countCarReservationsCreatedBetween(long ownerId, long carId, OffsetDateTime from, OffsetDateTime until);

    /** Car-centric variant of {@link #findListingNextActiveReservationDate}. */
    Optional<OffsetDateTime> findCarNextActiveReservationDate(long ownerId, long carId, OffsetDateTime after);

    /** Car-centric variant of {@link #findListingFinishedReservations}. */
    List<Reservation> findCarFinishedReservations(long ownerId, long carId);

    /**
     * Sets {@code car_returned}; {@code status} becomes {@code finished} only if {@code payment_approved} is already
     * true, otherwise unchanged.
     */
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
}
