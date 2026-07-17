package ar.edu.itba.paw.persistence.reservation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.models.util.search.ReservationSearchCriteria;

/** Reservations, overlap checks, analytics, and mail-claim flags. */
public interface ReservationDao {

    /** Returns true if any blocking reservation for the car overlaps the given range. */
    boolean hasActiveOverlapByCar(long carId, OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Same overlap query as {@link #hasActiveOverlapByCar}, but ignoring the reservation whose id matches
     * {@code excludingReservationId}. Used by the rider-side "edit pending reservation period" flow so the
     * reservation being edited does not collide against its own current dates.
     */
    boolean hasActiveOverlapByCarExcluding(
            long carId, OffsetDateTime startDate, OffsetDateTime endDate, long excludingReservationId);

    /** Blocking reservations ({@code pending}, {@code accepted}, {@code started}) for a car. */
    List<Reservation> findBlockingByCarId(long carId);

    /**
     * Batch variant of {@link #findBlockingByCarId} for many cars at once: same {@code pending},
     * {@code accepted}, {@code started} status filter, scoped to {@code carIds}. The caller is
     * responsible for grouping the rows by {@link Reservation#getCarId()} as needed. Returns an
     * empty list when {@code carIds} is null or empty.
     */
    List<Reservation> findBlockingByCarIds(Collection<Long> carIds);

    /**
     * Same as {@link #findBlockingByCarId}, but excludes the reservation whose id matches
     * {@code excludingReservationId}. Used to recompute the bookable wall-day calendar when a rider
     * is editing its own pending reservation (the reservation under edit must not subtract days
     * from its own bookable window).
     */
    List<Reservation> findBlockingByCarIdExcluding(long carId, long excludingReservationId);

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

    /**
     * Payment-proof deadline sweep: cancels only when the row is still {@link Reservation.Status#PENDING},
     * has no payment receipt, and the deadline has passed. Returns 0 when a concurrent receipt upload
     * already moved the row to {@code accepted} (or attached a receipt).
     */
    int cancelPendingMissingPaymentProofIfEligible(long reservationId, OffsetDateTime now);

    int attachPaymentReceiptAndAccept(long reservationId, long riderId, long storedFileId);

    Optional<Reservation> getReservationById(long id);

    Optional<Reservation> getOwnerReservationById(long ownerId, long reservationId);

    /**
     * Whether {@code riderId} has any reservation (any status) for {@code carId}.
     * Used so reservation riders can follow hypermedia {@code links.car} even when the car is
     * no longer publicly readable.
     */
    boolean existsByRiderIdAndCarId(long riderId, long carId);

    Page<ReservationCard> getRiderReservationCards(ReservationSearchCriteria criteria);

    List<Reservation> getReminderReservations(final OffsetDateTime from, final OffsetDateTime to);

    /**
     * Ids of {@link Reservation.Status#ACCEPTED} rows whose pickup {@code start_date} is on or before
     * {@code now} (UTC). Used by the scheduled job that transitions confirmed rentals to
     * {@link Reservation.Status#STARTED} once pickup time is reached.
     */
    List<Long> findAcceptedReservationIdsWithStartOnOrBefore(OffsetDateTime now);

    /**
     * When the row is still {@link Reservation.Status#ACCEPTED} and {@code startDate <= now}, sets
     * {@link Reservation.Status#STARTED} via dirty checking. Returns 0 if the row is absent, already
     * started, or pickup is still in the future.
     */
    int transitionAcceptedToStartedIfDue(long reservationId, OffsetDateTime now);

    Page<ReservationCard> getOwnerReservationCards(ReservationSearchCriteria criteria);

    int updateReservationStatus(long reservationId, String status);

    /**
     * Rider-driven edit of a pending unpaid reservation: replaces the rental period, total price and
     * payment-proof deadline through dirty checking on the managed {@link Reservation} entity.
     * Returns 0 when the row does not exist, is not owned by {@code riderId}, has already attached a
     * payment receipt or has left the {@link Reservation.Status#PENDING} state — in any of those cases
     * the row is left untouched.
     */
    int updateRiderPendingReservationPeriod(
            long reservationId,
            long riderId,
            OffsetDateTime newStartDate,
            OffsetDateTime newEndDate,
            BigDecimal newTotalPrice,
            OffsetDateTime newPaymentProofDeadlineAt);

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

    /**
     * Start/end bounds of finished reservations for analytics (no entity graph).
     * Each element is {@code [startDate, endDate]}.
     */
    List<OffsetDateTime[]> findCarFinishedReservationBounds(long ownerId, long carId);

    /**
     * Sum of wall-zone billable days across finished reservations for the car
     * (same calendar math as pricing {@code BillableDays}).
     */
    long sumCarFinishedBillableDays(long ownerId, long carId);

    /** Sets {@code car_returned} and stamps {@code car_returned_at = now()}; transitions {@code status} to {@code finished} when the car is marked returned. */
    int markCarReturned(long reservationId, long ownerUserId);

    List<Reservation> findReservationsForReturnReminderEmail(OffsetDateTime now, int hoursBeforeCheckout);

    List<Reservation> findReservationsForReturnCheckoutEmail(OffsetDateTime now);

    List<Reservation> findReservationsForRiderReviewInviteEmail(OffsetDateTime now);

    /**
     * Reservations whose rental period ({@code end_date}) ended at or before {@code endDateCutoff}, with no
     * rider-side review yet. Used by the review auto-skip scheduler to insert a "skipped" review row after
     * a configurable grace window. Only statuses that allow a rider review ({@code accepted},
     * {@code started}, {@code finished}) are considered.
     */
    List<Reservation> findReservationsForRiderReviewAutoSkip(OffsetDateTime now, OffsetDateTime endDateCutoff);

    /**
     * Reservations whose car was marked returned at or before {@code carReturnedAtCutoff}, with no
     * owner-side review yet. Used by the review auto-skip scheduler. Only statuses where the owner form
     * is visible ({@code accepted}, {@code started}, {@code finished}) are considered.
     */
    List<Reservation> findReservationsForOwnerReviewAutoSkip(OffsetDateTime now, OffsetDateTime carReturnedAtCutoff);

    /** Sets {@code return_reminder_email_sent} if still false; returns rows updated (0 or 1). */
    int claimReturnReminderEmailSent(long reservationId);

    /** Sets {@code pickup_reminder_email_sent} if still false; returns rows updated (0 or 1). */
    int claimPickupReminderEmailSent(long reservationId);

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
     * Returns 0 when the row is absent, not in a cancellable state, or a concurrent receipt upload won the race.
     */
    int updateParticipantCancellationWithRefundMeta(
            long reservationId,
            String statusLower,
            boolean paymentRefundRequired,
            OffsetDateTime refundProofDeadlineAtOrNull);

    /**
     * Admin car-pause cascade: cancels blocking reservations with the correct terminal status
     * ({@code cancelled_due_to_missing_payment_proof} only for unpaid pending rows).
     */
    int applyAdminCarPauseCancellation(
            long reservationId,
            boolean paymentRefundRequired,
            OffsetDateTime refundProofDeadlineAtOrNull);

    int attachRefundReceipt(long reservationId, long ownerUserId, long storedFileId);

    List<Reservation> findReservationsWithDuePendingRefundProof(OffsetDateTime now);

    int claimPendingRefundEmailSent(long reservationId);

    /**
     * Reservations whose refund-proof deadline has already passed without an uploaded receipt.
     * Fetches {@code car.owner} eagerly because callers group by owner and dispatch mails per owner.
     */
    List<Reservation> findReservationsWithOverdueRefundProof(OffsetDateTime now);

    /**
     * Count of pending refund proofs (deadline already passed, no receipt) belonging to the cars of
     * {@code ownerUserId}. Used to decide whether the owner can be unblocked after uploading a proof.
     */
    long countOverdueRefundProofsForOwner(long ownerUserId, OffsetDateTime now);

    /**
     * Reservations with an overdue refund-proof for {@code ownerUserId}. Returns the entities (JPQL
     * "entities not tables" rule) so callers can read whatever fields they need — the navbar banner
     * advice maps these to ids to deep-link the CTA when exactly one is pending. Ordered by deadline
     * ascending so the first row is the most overdue one. In practice a blocked owner has a very small
     * number of pending refund proofs (typically 1), so loading entities here is cheap.
     */
    List<Reservation> findOverdueRefundProofReservationsForOwner(long ownerUserId, OffsetDateTime now);

    /**
     * Batch variant of {@link #findOverdueRefundProofReservationsForOwner}: maps each owner id to the
     * ordered list of overdue refund-proof reservation ids (same ordering as the single-owner query).
     */
    Map<Long, List<Long>> findOverdueRefundProofReservationIdsByOwnerIds(
            Collection<Long> ownerUserIds, OffsetDateTime now);

    /**
     * Reservations of {@code ownerUserId} that still require a refund-proof upload (no receipt yet),
     * irrespective of whether the deadline has already lapsed. Used by the owner-side hub views
     * (my-cars, owner reservations) to surface a "you must upload a refund receipt" badge for the
     * affected cars/reservations, regardless of whether the owner is already blocked.
     */
    List<Reservation> findReservationsRequiringRefundProofForOwner(long ownerUserId);

    /** All reservations paginated for admin view; ordered by creation date descending. */
    Page<ReservationCard> findAllReservationCards(int page, int pageSize);

    /** Single card projection for teaser reads ({@code GET /reservations/{id}} with summary MIME). */
    Optional<ReservationCard> findReservationCardById(long reservationId);
}
