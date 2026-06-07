package ar.edu.itba.paw.services.reservation;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.models.util.search.ReservationSearchCriteria;

/**
 * Reservation lifecycle, pricing, participant checks, and mail side effects. The implementation uses only
 * {@code ReservationDao}; user and car data are resolved through peer services.
 */
public interface ReservationService {

    /** Loads a reservation by id when present (no participant check). */
    Optional<Reservation> getReservationById(long id);

    /** Reservation row only if {@code riderId} is the rider on that reservation. */
    Optional<Reservation> getRiderReservationById(long riderId, long reservationId);

    /** Reservation row only if {@code ownerId} owns the car tied to that reservation. */
    Optional<Reservation> getOwnerReservationById(long ownerId, long reservationId);

    /** Paginated rider “my reservations” cards from sanitized criteria. */
    Page<ReservationCard> getRiderReservationCards(ReservationSearchCriteria criteria);

    /** Paginated owner “my reservations” cards from sanitized criteria. */
    Page<ReservationCard> getOwnerReservationCards(ReservationSearchCriteria criteria);

    /**
     * Builds {@link ReservationSearchCriteria} from list/search parameters (filters, sort,
     * page index and page size supplied by the caller). Pass {@code carId} to restrict to a
     * single car (owner hub); pass {@code null} for all cars.
     */
    ReservationSearchCriteria buildReservationSearchCriteria(
            Long ownerId,
            Long riderId,
            List<Car.Type> category,
            List<Car.Transmission> transmission,
            List<Car.Powertrain> powertrain,
            BigDecimal priceMin,
            BigDecimal priceMax,
            List<String> rating,
            List<Reservation.Status> statusFilter,
            int page,
            int pageSize,
            String sort,
            String textQuery,
            Long carId);

    /**
     * Normalizes a client-supplied total string (digits and optional single decimal point); empty when invalid.
     */
    Optional<String> normalizeClientReservationTotal(String reservationTotal);

    /**
     * Formatted total for UI when {@code carId} and wall-local range parse; uses car availability day price.
     */
    Optional<String> reservationTotalDisplayByCar(Long carId, String fromDateTime, String untilDateTime);

    /**
     * Rider reservation from the web form: validate {@code riderId}, car/dates/availability, then create the reservation.
     *
     * @throws ar.edu.itba.paw.exception.reservation.RiderReservationException for validation errors (message key in {@link ar.edu.itba.paw.exception.MessageKeys})
     * @throws ar.edu.itba.paw.exception.reservation.ReservationConflictException when the interval overlaps existing reservations
     */
    Reservation submitRiderReservationByCar(
            long riderId,
            long carId,
            Long availabilityId,
            String fromDateTime,
            String untilDateTime);

    /**
     * Rider-driven edit of the rental period of an unpaid pending reservation. Re-validates the new
     * window like a fresh submission (date order, pickup not in the past, configured pickup lead,
     * fits within the car's offered availability, handover times match the effective availability,
     * billable days within {@link #getConfiguredMaxReservationBillableDays}, billable days {@code >=}
     * the car's minimum rental days, no overlap with other blocking reservations), re-computes the
     * total price from the per-day plan, replaces the covering availability links and resets the
     * payment-proof deadline relative to the moment of the edit.
     *
     * @throws ar.edu.itba.paw.exception.reservation.RiderReservationException for validation errors
     * @throws ar.edu.itba.paw.exception.reservation.ReservationConflictException when the new interval
     *         overlaps another blocking reservation for the same car
     */
    Reservation editPendingReservationByRider(
            long riderId,
            long reservationId,
            String fromDateTime,
            String untilDateTime);

    /** Total price for the car and UTC interval using configured billable-day rules; empty when inputs are invalid. */
    Optional<BigDecimal> calculateTotalByCar(long carId, OffsetDateTime startDate, OffsetDateTime endDate);

    /** Inclusive billable rental days from pickup/return instants in the wall zone (at least one when valid). */
    long calculateBillableDays(OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Cancels without participant checks (e.g. expired pending payment job). Persists
     * {@link ar.edu.itba.paw.models.domain.reservation.Reservation.Status#CANCELLED_DUE_TO_MISSING_PAYMENT_PROOF}. Prefer
     * {@link #cancelReservationAsParticipant(long, long)} for user-initiated cancellations.
     */
    Optional<Reservation> cancelReservation(long reservationId);

    /**
     * Rider or owner may cancel {@code PENDING} without payment receipt, or either may cancel {@code ACCEPTED}
     * before pickup ({@code start_date}). Confirmed cancellations with a prior payment receipt require the owner to
     * upload a refund proof (see refund columns). Other states are rejected.
     */
    Optional<Reservation> cancelReservationAsParticipant(long userId, long reservationId);

    /**
     * Role-scoped variant of {@link #cancelReservationAsParticipant(long, long)} that performs the access
     * pre-check internally, eliminating the double {@code getOwnerReservationById}/{@code getRiderReservationById}
     * call that controllers used to do before invoking the cancel API. Either {@code "rider"} or {@code "owner"}
     * is accepted; any other value is treated as access denied.
     *
     * @param viewerUserId  id of the signed-in user attempting the cancellation
     * @param reservationId reservation primary key
     * @param viewerRole    {@code "rider"} or {@code "owner"} — the hat the viewer wears for this request
     * @return the cancelled reservation row (with refreshed state)
     * @throws ar.edu.itba.paw.exception.reservation.ReservationAccessDeniedException when the reservation does not
     *         exist, the role is unknown, or the user is not a participant under {@code viewerRole}
     * @throws ar.edu.itba.paw.exception.reservation.ReservationCancelNotAllowedException when the reservation exists
     *         and the viewer has access but the current state does not allow self-cancellation
     */
    Reservation cancelReservationAsParticipantScoped(long viewerUserId, long reservationId, String viewerRole);

    /**
     * Batch job: cancels pending reservations whose payment-proof deadline passed without a receipt, and notifies.
     */
    void cancelExpiredPendingPaymentReservations();

    /**
     * Rider uploads a payment receipt file; validates rider, state, and size policy, persists file metadata, and may
     * notify the owner.
     */
    void attachPaymentReceipt(long riderId, long reservationId, String originalFilename, String contentType, byte[] data);

    /**
     * Payment proof if the user is the rider or the owner of the car and the reservation has an associated file.
     */
    Optional<StoredFile> findPaymentReceiptForParticipant(long userId, long reservationId);

    /** {@link BinaryContent} variant of {@link #findPaymentReceiptForParticipant} for download endpoints (issue #16). */
    Optional<BinaryContent> findPaymentReceiptContentForParticipant(long userId, long reservationId);

    /** Owner uploads refund transfer proof for a cancelled confirmed reservation that requires refund documentation. */
    void attachRefundReceiptByOwner(long ownerUserId, long reservationId, String originalFilename, String contentType, byte[] data);

    /** Refund receipt file when the viewer is rider or owner on a cancelled reservation that has one. */
    Optional<StoredFile> findRefundReceiptForParticipant(long userId, long reservationId);

    /** {@link BinaryContent} variant of {@link #findRefundReceiptForParticipant} for download endpoints (issue #16). */
    Optional<BinaryContent> findRefundReceiptContentForParticipant(long userId, long reservationId);

    /** Minimum hours between "now" and the pickup ({@code app.reservation.pickup-lead-hours}). */
    int getConfiguredPickupLeadHours();

    /** Hours to upload payment proof after creating a pending reservation ({@code app.reservation.payment-proof-deadline-hours}). */
    int getConfiguredPaymentProofDeadlineHours();

    /** Owner car detail: paginated reservation cards for one car, optional status filter token. */
    Page<ReservationCard> getCarReservationCards(long ownerId, long carId, int page, int pageSize, String statusFilter);

    /** Counts reservations per status bucket for the owner's car dashboard charts. */
    Map<String, Long> countCarReservationsByStatus(long ownerId, long carId);

    /** Sum of {@code total_price} for reservations in {@code accepted}, {@code started}, or {@code finished} states (car-centric). */
    BigDecimal getCarTotalEarnings(long ownerId, long carId);

    /** Sum of {@code total_price} for reservations in {@code accepted} or {@code started} states (in-flight revenue). */
    BigDecimal getCarPendingEarnings(long ownerId, long carId);

    /** Sum of billable days across {@code finished} reservations for that car (owner analytics). */
    long getCarTotalDaysRented(long ownerId, long carId);

    /** Count of reservations for that car whose {@code created_at} falls in the current UTC calendar month. */
    long getCarReservationsThisMonth(long ownerId, long carId);

    /**
     * Earliest {@code start_date} of an {@code accepted} or {@code started} reservation on that car strictly after
     * {@code now} (UTC), if any.
     */
    Optional<OffsetDateTime> getCarNextReservationDate(long ownerId, long carId);

    /**
     * Owner confirms vehicle returned after checkout; irreversible. When both return and payment approval hold,
     * {@code status} becomes {@code finished}, otherwise {@code accepted}/{@code started}.
     */
    void markCarReturnedByOwner(long ownerUserId, long reservationId);

    /** Hours before {@code end_date} to email the rider to return the vehicle ({@code app.reservation.return-reminder-hours-before-checkout}). */
    int getConfiguredReturnReminderHoursBeforeCheckout();

    /** Max inclusive billable days for one reservation ({@code app.reservation.max-billable-days}). */
    int getConfiguredMaxReservationBillableDays();

    /** Scheduled job: reminder email to return the car (within configured hours before checkout). */
    void dispatchReturnReminderEmails();

    /** Scheduled job: email at checkout if the car was not marked returned. */
    void dispatchReturnCheckoutEmails();

    /** Scheduled job: invite the rider to leave an optional review after the rental period. */
    void dispatchRiderReviewInviteEmails();

    /**
     * Scheduled job: closes stale reviews by inserting a null/commentless "skipped" review row. Rider
     * window starts at the reservation {@code endDate}; owner window starts at the moment the owner
     * marked the car returned. Window length is {@code app.reservation.review-auto-skip-days} (a value
     * less than 1 disables the job). Idempotent — a reservation with an existing review on that side
     * is skipped, and any per-row failure is logged and the loop continues.
     */
    void dispatchReviewAutoSkips();

    /**
     * Scheduled job: sends payment-proof deadline reminders to riders whose deadline falls within the configured lead
     * window ({@code app.reservation.payment-proof-reminder-lead-hours}).
     */
    void dispatchDuePaymentProofReminderEmails();

    /**
     * Scheduled job: reminds the host to upload refund transfer proof before the deadline (same lead window as payment
     * proof reminders).
     */
    void dispatchDueRefundProofReminderEmails();

    /**
     * Scheduled job: blocks owners whose refund-proof deadlines have already lapsed without an uploaded receipt.
     * Idempotent — owners already blocked are skipped. A single email is enqueued per newly-blocked owner with
     * the full list of overdue reservations.
     */
    void sweepRefundOverdueAndBlockOwners();

    /**
     * Reservations in {@code pending}, {@code accepted}, or {@code started} for one car (availability overlap checks).
     */
    List<Reservation> findBlockingReservationsByCarId(long carId);

    /**
     * Same as {@link #findBlockingReservationsByCarId(long)} but excludes the reservation whose id
     * matches {@code excludingReservationId}. Used to recompute the bookable wall-day calendar for the
     * rider-side reservation edit flow (the reservation under edit must not subtract days from its
     * own bookable window).
     */
    List<Reservation> findBlockingReservationsByCarIdExcluding(long carId, long excludingReservationId);

    /**
     * Blocking reservations for {@code carId} whose date range intersects {@code [from, to)} (UTC).
     * Used by the owner availability-edit flow to decide whether withdrawing a set of days would
     * conflict with an already-active reservation.
     */
    List<Reservation> findBlockingReservationsByCarIdInRange(long carId, OffsetDateTime from, OffsetDateTime to);

    /**
     * Reservations whose pickup {@code start_date} lies in {@code [from, to)} (UTC), for the day-before reminder
     * scheduled job.
     */
    List<Reservation> findReminderReservations(OffsetDateTime from, OffsetDateTime to);

    // -----------------------------------------------------------------------------------------------------------
    // Admin-orchestrated read of reservation rows.
    //
    // Exists so that {@link AdminService} can list every reservation in the system without bypassing the layering
    // rule "each service may only call its own DAO".
    // -----------------------------------------------------------------------------------------------------------

    /** Admin-only: paginated list of every reservation in the system as display cards. */
    Page<ReservationCard> findAllReservationCards(int page, int pageSize);

    /**
     * Identifiers of reservations whose refund-proof deadline has lapsed for {@code ownerUserId} (no receipt yet).
     * Used by the navbar blocked-account banner to deep-link the CTA at the single reservation when only one
     * is pending. Ordered by deadline ascending (most overdue first).
     */
    List<Long> findOverdueRefundProofReservationIdsForOwner(long ownerUserId);

    /**
     * Reservation ids belonging to {@code ownerUserId} that still require a refund proof upload (no receipt
     * yet), independently of whether the deadline has lapsed. Used by owner-side hub views to render a
     * per-reservation "you must upload a refund receipt" badge.
     */
    Set<Long> findOwnerReservationIdsRequiringRefundProof(long ownerUserId);

    /**
     * Car ids of {@code ownerUserId} that have at least one reservation still requiring a refund proof.
     * Used by the {@code /my-cars} grid to surface a per-car "you have a reservation requiring a refund
     * receipt" badge regardless of whether the owner is already blocked.
     */
    Set<Long> findOwnerCarIdsWithReservationRequiringRefundProof(long ownerUserId);

    // -----------------------------------------------------------------------------------------------------------
    // Sub-service-orchestrated operations on reservation rows.
    //
    // These methods exist so that {@link ReservationQueryService}, {@link ReservationWorkflowService},
    // {@link ReservationPaymentService}, and {@link ReservationLifecycleSchedulerService} can mutate / read
    // reservation rows without bypassing the layering rule "each ServiceImpl may only call its own DAO".
    // They are intentionally narrow pass-throughs to {@code ReservationDao} (no business rules, no policy
    // checks, no side effects). The calling sub-service owns those concerns.
    // -----------------------------------------------------------------------------------------------------------

    /** Returns true if any blocking reservation for the car overlaps the given range. */
    boolean hasActiveOverlapByCar(long carId, OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Same overlap check as {@link #hasActiveOverlapByCar} ignoring the reservation under edit.
     * Used by the rider edit flow so the reservation under edit does not collide with itself.
     */
    boolean hasActiveOverlapByCarExcluding(
            long carId, OffsetDateTime startDate, OffsetDateTime endDate, long excludingReservationId);

    /** Creates a reservation row for {@code carId} with the given dates / status / pricing / deadline. */
    Reservation createReservationForCar(
            long riderId,
            long carId,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Reservation.Status status,
            BigDecimal totalPrice,
            OffsetDateTime paymentProofDeadlineAt);

    /** Updates {@code status} for the reservation row (lower-case string token). Returns rows affected. */
    int updateReservationStatus(long reservationId, String status);

    /**
     * Rider-driven edit of a pending unpaid reservation row: replaces the rental period, total price and
     * payment-proof deadline through dirty checking. Returns 0 when the row does not exist, is not owned
     * by {@code riderId}, has already attached a payment receipt, or has left {@code PENDING}.
     */
    int updateRiderPendingReservationPeriod(
            long reservationId,
            long riderId,
            OffsetDateTime newStartDate,
            OffsetDateTime newEndDate,
            BigDecimal newTotalPrice,
            OffsetDateTime newPaymentProofDeadlineAt);

    /**
     * Participant cancellation row mutation: status plus optional refund obligation (confirmed reservation
     * with prior payment). Returns rows affected.
     */
    int updateParticipantCancellationWithRefundMeta(
            long reservationId,
            String statusLower,
            boolean paymentRefundRequired,
            OffsetDateTime refundProofDeadlineAtOrNull);

    /** Marks the car as returned and transitions status to {@code finished} when the gate holds. */
    int markCarReturned(long reservationId, long ownerUserId);

    /** Attaches a payment receipt to the reservation and flips status to {@code accepted}. */
    int attachPaymentReceiptAndAccept(long reservationId, long riderId, long storedFileId);

    /** Attaches a refund receipt to a cancelled-with-refund reservation. */
    int attachRefundReceipt(long reservationId, long ownerUserId, long storedFileId);

    /** Pending reservations whose payment-proof deadline has lapsed (UTC). */
    List<Reservation> findPendingPaymentPastDeadline(OffsetDateTime now);

    /** Reservations that should receive the return-reminder email within the configured hours-before-checkout. */
    List<Reservation> findReservationsForReturnReminderEmail(OffsetDateTime now, int hoursBeforeCheckout);

    /** Reservations that should receive the checkout email (end_date reached, no return yet). */
    List<Reservation> findReservationsForReturnCheckoutEmail(OffsetDateTime now);

    /** Reservations that should receive the rider-review invite email. */
    List<Reservation> findReservationsForRiderReviewInviteEmail(OffsetDateTime now);

    /** Reservations whose rider review window has lapsed without a review. Used by the auto-skip scheduler. */
    List<Reservation> findReservationsForRiderReviewAutoSkip(OffsetDateTime now, OffsetDateTime endDateCutoff);

    /** Reservations whose owner review window has lapsed without a review. Used by the auto-skip scheduler. */
    List<Reservation> findReservationsForOwnerReviewAutoSkip(OffsetDateTime now, OffsetDateTime carReturnedAtCutoff);

    /** Sets {@code return_reminder_email_sent} idempotently. Returns rows updated (0 or 1). */
    int claimReturnReminderEmailSent(long reservationId);

    /** Sets {@code return_checkout_email_sent} idempotently. Returns rows updated (0 or 1). */
    int claimReturnCheckoutEmailSent(long reservationId);

    /** Sets {@code rider_review_invite_email_sent} idempotently. Returns rows updated (0 or 1). */
    int claimRiderReviewInviteEmailSent(long reservationId);

    /** Reservations whose payment-proof deadline falls inside the reminder lead window. */
    List<Reservation> findReservationsWithDuePendingPaymentProof(OffsetDateTime now);

    /** Sets {@code pending_paymentproof_email_sent} idempotently. Returns rows updated (0 or 1). */
    int claimPendingPaymentProofEmailSent(long reservationId);

    /** Reservations whose refund-proof deadline falls inside the reminder lead window. */
    List<Reservation> findReservationsWithDuePendingRefundProof(OffsetDateTime now);

    /** Sets {@code pending_refundproof_email_sent} idempotently. Returns rows updated (0 or 1). */
    int claimPendingRefundEmailSent(long reservationId);

    /** Reservations whose refund-proof deadline has already lapsed without an uploaded receipt. */
    List<Reservation> findReservationsWithOverdueRefundProof(OffsetDateTime now);

    /** Count of pending refund proofs (deadline already lapsed, no receipt) belonging to the cars of {@code ownerUserId}. */
    long countOverdueRefundProofsForOwner(long ownerUserId, OffsetDateTime now);

    /** Entities whose refund-proof deadline has lapsed without a receipt for {@code ownerUserId}, ordered by deadline ascending. */
    List<Reservation> findOverdueRefundProofReservationsForOwner(long ownerUserId, OffsetDateTime now);

    /** Reservations of {@code ownerUserId} that still require a refund-proof upload (no receipt yet). */
    List<Reservation> findReservationsRequiringRefundProofForOwner(long ownerUserId);

    /** Sum of {@code total_price} for reservations in the given statuses, scoped to a car. */
    BigDecimal sumCarRevenueByStatuses(long ownerId, long carId, Collection<String> statuses);

    /** Count of reservations for a car created within {@code [from, until)}. */
    long countCarReservationsCreatedBetween(long ownerId, long carId, OffsetDateTime from, OffsetDateTime until);

    /** Earliest {@code start_date} of an accepted/started reservation on the car strictly after {@code after}. */
    Optional<OffsetDateTime> findCarNextActiveReservationDate(long ownerId, long carId, OffsetDateTime after);

    /** Finished reservations for the car (owner analytics). */
    List<Reservation> findCarFinishedReservations(long ownerId, long carId);
}
