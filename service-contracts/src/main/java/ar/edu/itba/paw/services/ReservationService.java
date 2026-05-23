package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.dto.ReservationCard;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.util.ReservationSearchCriteria;

/**
 * Reservation lifecycle, pricing, participant checks, and mail side effects. The implementation uses only
 * {@code ReservationDao}; user and listing data are resolved through peer services.
 */
public interface ReservationService {

    /**
     * Persists a new reservation after overlap and business checks; may enqueue confirmation mail.
     */
    Reservation createReservation(
            long riderId,
            long listingId,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Reservation.Status status,
            OffsetDateTime paymentProofDeadlineAt);

    /** Loads a reservation by id when present (no participant check). */
    Optional<Reservation> getReservationById(long id);

    /** Reservation row only if {@code riderId} is the rider on that reservation. */
    Optional<Reservation> getRiderReservationById(long riderId, long reservationId);

    /** Reservation row only if {@code ownerId} owns the listing tied to that reservation. */
    Optional<Reservation> getOwnerReservationById(long ownerId, long reservationId);

    /** Paginated rider “my reservations” cards from sanitized criteria. */
    Page<ReservationCard> getRiderReservationCards(ReservationSearchCriteria criteria);

    /** Paginated owner “my reservations” cards from sanitized criteria. */
    Page<ReservationCard> getOwnerReservationCards(ReservationSearchCriteria criteria);

    /**
     * Builds {@link ReservationSearchCriteria} from list/search parameters (filters, sort, default page size from
     * configuration).
     */
    ReservationSearchCriteria buildReservationSearchCriteria(
            Long ownerId,
            Long riderId,
            List<String> category,
            List<String> transmission,
            List<String> powertrain,
            BigDecimal priceMin,
            BigDecimal priceMax,
            List<String> rating,
            List<String> statusFilter,
            int page,
            String sort,
            String textQuery);

    /**
     * Normalizes a client-supplied total string (digits and optional single decimal point); empty when invalid.
     */
    Optional<String> normalizeClientReservationTotal(String reservationTotal);

    /**
     * Formatted total for UI when {@code listingId} and wall-local range parse; uses listing day price and billable days.
     */
    Optional<String> reservationTotalDisplay(Long listingId, String fromDateTime, String untilDateTime);

    /**
     * Formatted total for UI when {@code carId} and wall-local range parse; uses car availability day price.
     */
    Optional<String> reservationTotalDisplayByCar(Long carId, String fromDateTime, String untilDateTime);

    /**
     * Rider reservation from the web form: validate {@code riderId}, listing/dates/availability, then {@link #createReservation}.
     *
     * @throws ar.edu.itba.paw.exception.reservation.RiderReservationException for validation errors (message key in {@link ar.edu.itba.paw.exception.MessageKeys})
     * @throws ar.edu.itba.paw.exception.reservation.ReservationConflictException when the interval overlaps existing reservations
     */
    Reservation submitRiderReservation(
            long riderId,
            Long listingId,
            Long availabilityId,
            String fromDateTime,
            String untilDateTime);

    /**
     * Like {@link #submitRiderReservation} but resolves the reservation via {@code carId} instead of a listing.
     * New reservations created through this method do not have {@code listing_id} set.
     */
    Reservation submitRiderReservationByCar(
            long riderId,
            long carId,
            Long availabilityId,
            String fromDateTime,
            String untilDateTime);

    /** Total price for the listing and UTC interval using configured billable-day rules; empty when inputs are invalid. */
    Optional<BigDecimal> calculateTotal(long listingId, OffsetDateTime startDate, OffsetDateTime endDate);

    /** Total price for the car and UTC interval using configured billable-day rules; empty when inputs are invalid. */
    Optional<BigDecimal> calculateTotalByCar(long carId, OffsetDateTime startDate, OffsetDateTime endDate);

    /** Inclusive billable rental days from pickup/return instants in the wall zone (at least one when valid). */
    long calculateBillableDays(OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Cancels without participant checks (e.g. expired pending payment job). Persists
     * {@link ar.edu.itba.paw.models.domain.Reservation.Status#CANCELLED_DUE_TO_MISSING_PAYMENT_PROOF}. Prefer
     * {@link #cancelReservationAsParticipant(long, long)} for user-initiated cancellations.
     */
    Optional<Reservation> cancelReservation(long reservationId);

    /**
     * Rider or owner may cancel {@code PENDING} without payment receipt, or either may cancel {@code ACCEPTED}
     * before pickup ({@code start_date}). Confirmed cancellations with a prior payment receipt require the owner to
     * upload a refund proof (see refund columns). Other states are rejected.
     */
    Optional<Reservation> cancelReservationAsParticipant(long userId, long reservationId);

    /** Reservations for this listing in {@code pending}, {@code accepted}, or {@code started} (e.g. listing deletion). */
    List<Reservation> getReservationsForCancellation(final long listingId);

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

    /**
     * Owner validates the payment proof (only accepted/started/finished reservation with proof); irreversible.
     */
    void approvePaymentReceiptByOwner(long ownerUserId, long reservationId);

    /** Owner uploads refund transfer proof for a cancelled confirmed reservation that requires refund documentation. */
    void attachRefundReceiptByOwner(long ownerUserId, long reservationId, String originalFilename, String contentType, byte[] data);

    /** Refund receipt file when the viewer is rider or owner on a cancelled reservation that has one. */
    Optional<StoredFile> findRefundReceiptForParticipant(long userId, long reservationId);

    /** Rider validates the refund transfer receipt uploaded by the host. */
    void setPaymentRefundApprovalByRider(long riderUserId, long reservationId, boolean approved);

    /** Minimum hours between "now" and the pickup ({@code app.reservation.pickup-lead-hours}). */
    int getConfiguredPickupLeadHours();

    /** Hours to upload payment proof after creating a pending reservation ({@code app.reservation.payment-proof-deadline-hours}). */
    int getConfiguredPaymentProofDeadlineHours();

    /** Owner listing detail: paginated reservation cards for one listing, optional status filter token. */
    Page<ReservationCard> getListingReservationCards(long ownerId, long listingId, int page, int pageSize, String statusFilter);

    /** Owner car detail: paginated reservation cards for one car, optional status filter token. */
    Page<ReservationCard> getCarReservationCards(long ownerId, long carId, int page, int pageSize, String statusFilter);

    /** Counts reservations per status bucket for the owner’s listing dashboard charts. */
    Map<String, Long> countListingReservationsByStatus(long ownerId, long listingId);

    /** Counts reservations per status bucket for the owner's car dashboard charts. */
    Map<String, Long> countCarReservationsByStatus(long ownerId, long carId);

    /** Sum of {@code total_price} for reservations in {@code accepted}, {@code started}, or {@code finished} states. */
    BigDecimal getListingTotalEarnings(long ownerId, long listingId);

    /** Sum of {@code total_price} for reservations in {@code accepted} or {@code started} states (in-flight revenue). */
    BigDecimal getListingPendingEarnings(long ownerId, long listingId);

    /** Sum of billable days across {@code finished} reservations for that listing (owner analytics). */
    long getListingTotalDaysRented(long ownerId, long listingId);

    /** Count of reservations for that listing whose {@code created_at} falls in the current UTC calendar month. */
    long getListingReservationsThisMonth(long ownerId, long listingId);

    /**
     * Earliest {@code start_date} of an {@code accepted} or {@code started} reservation on that listing strictly after
     * {@code now} (UTC), if any.
     */
    Optional<OffsetDateTime> getListingNextReservationDate(long ownerId, long listingId);

    /** Car-centric variant of {@link #getListingTotalEarnings}. */
    BigDecimal getCarTotalEarnings(long ownerId, long carId);

    /** Car-centric variant of {@link #getListingPendingEarnings}. */
    BigDecimal getCarPendingEarnings(long ownerId, long carId);

    /** Car-centric variant of {@link #getListingTotalDaysRented}. */
    long getCarTotalDaysRented(long ownerId, long carId);

    /** Car-centric variant of {@link #getListingReservationsThisMonth}. */
    long getCarReservationsThisMonth(long ownerId, long carId);

    /** Car-centric variant of {@link #getListingNextReservationDate}. */
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
     * Reservations in {@code pending}, {@code accepted}, or {@code started} for one listing (availability overlap checks).
     */
    List<Reservation> findBlockingReservationsByListingId(long listingId);

    /** Same as {@link #findBlockingReservationsByListingId(long)} for a batch of listings. */
    List<Reservation> findBlockingReservationsByListingIds(Collection<Long> listingIds);

    /** Like {@link #findBlockingReservationsByListingId(long)} but resolves by {@code car_id} (Phase 7d+). */
    List<Reservation> findBlockingReservationsByCarId(long carId);

    /**
     * Blocking reservations for {@code listingId} whose date range intersects {@code [from, to)} (UTC).
     * Used by the owner availability-edit flow to decide whether withdrawing a set of days would
     * conflict with an already-active reservation.
     */
    List<Reservation> findBlockingReservationsByListingIdInRange(long listingId, OffsetDateTime from, OffsetDateTime to);

    /**
     * Like {@link #findBlockingReservationsByListingIdInRange} but resolves by {@code car_id}.
     * Required for the car-centric availability edit/withdraw conflict check (Phase 7c+).
     */
    List<Reservation> findBlockingReservationsByCarIdInRange(long carId, OffsetDateTime from, OffsetDateTime to);

    /**
     * Reservations whose pickup {@code start_date} lies in {@code [from, to)} (UTC), for the day-before reminder
     * scheduled job.
     */
    List<Reservation> findReminderReservations(OffsetDateTime from, OffsetDateTime to);
}
