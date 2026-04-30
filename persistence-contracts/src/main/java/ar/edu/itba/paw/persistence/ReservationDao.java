package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;



import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.dto.ReservationCard;
import ar.edu.itba.paw.models.util.ReservationSearchCriteria;

public interface ReservationDao {

    boolean hasActiveOverlap(long listingId, OffsetDateTime startDate, OffsetDateTime endDate);

    List<Reservation> findBlockingByListingId(long listingId);

    List<Reservation> findBlockingByListingIds(Collection<Long> listingIds);

    Reservation createReservation(
            long riderId,
            long listingId,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Reservation.Status status,
            BigDecimal totalPrice,
            OffsetDateTime paymentProofDeadlineAt);

    List<Reservation> findPendingPaymentPastDeadline(OffsetDateTime now);

    int attachPaymentReceiptAndAccept(long reservationId, long riderId, long storedFileId);

    int updatePaymentApproved(long reservationId, long ownerUserId, boolean approved);

    Optional<Reservation> getReservationById(long id);

    Optional<Reservation> getOwnerReservationById(long ownerId, long reservationId);

    Page<ReservationCard> getRiderReservationCards(ReservationSearchCriteria criteria);

    List<Reservation> getReminderReservations(final OffsetDateTime from, final OffsetDateTime to);

    Page<ReservationCard> getOwnerReservationCards(ReservationSearchCriteria criteria);

    int updateReservationStatus(long reservationId, String status);

    List<Reservation> getListingActiveReservations(long listingId);

    Page<ReservationCard> getListingReservationCards(long ownerId, long listingId, int page, int pageSize, String statusFilter);

    Map<String, Long> countListingReservationsByStatus(long ownerId, long listingId);

    BigDecimal sumListingRevenueByStatuses(long ownerId, long listingId, Collection<String> statuses);

    long countListingReservationsCreatedBetween(long ownerId, long listingId, OffsetDateTime from, OffsetDateTime until);

    Optional<OffsetDateTime> findListingNextActiveReservationDate(long ownerId, long listingId, OffsetDateTime after);

    List<Reservation> findListingFinishedReservations(long ownerId, long listingId);

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
}
