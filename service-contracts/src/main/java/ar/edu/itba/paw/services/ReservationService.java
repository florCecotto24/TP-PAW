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
            String sort);

    /**
     * Normalizes a client-supplied total string (digits and optional single decimal point); empty when invalid.
     */
    Optional<String> normalizeClientReservationTotal(String reservationTotal);

    /**
     * Formatted total for UI when {@code listingId} and wall-local range parse; uses listing day price and billable days.
     */
    Optional<String> reservationTotalDisplay(Long listingId, String fromDateTime, String untilDateTime);

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

    /** Total price for the listing and UTC interval using configured billable-day rules; empty when inputs are invalid. */
    Optional<BigDecimal> calculateTotal(long listingId, OffsetDateTime startDate, OffsetDateTime endDate);

    /** Inclusive billable rental days from pickup/return instants in the wall zone (at least one when valid). */
    long calculateBillableDays(OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Cancels without participant checks (e.g. expired pending payment job). Persists
     * {@link ar.edu.itba.paw.models.domain.Reservation.Status#CANCELLED_DUE_TO_MISSING_PAYMENT_PROOF}. Prefer
     * {@link #cancelReservationAsParticipant(long, long)} for user-initiated cancellations.
     */
    Optional<Reservation> cancelReservation(long reservationId);

    /**
     * Rider or owner may cancel {@code PENDING} while no payment receipt is attached, or either may cancel
     * {@code ACCEPTED}. Other states are rejected; participants are verified (rider/owner) before applying rules.
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
     * Set if the owner validated the payment proof (only accepted reservation with proof).
     */
    void setPaymentReceiptApprovalByOwner(long ownerUserId, long reservationId, boolean approved);

    /** Minimum hours between "now" and the pickup ({@code app.reservation.pickup-lead-hours}). */
    int getConfiguredPickupLeadHours();

    /** Hours to upload payment proof after creating a pending reservation ({@code app.reservation.payment-proof-deadline-hours}). */
    int getConfiguredPaymentProofDeadlineHours();

    /** Owner listing detail: paginated reservation cards for one listing, optional status filter token. */
    Page<ReservationCard> getListingReservationCards(long ownerId, long listingId, int page, int pageSize, String statusFilter);

    /** Counts reservations per status bucket for the owner’s listing dashboard charts. */
    Map<String, Long> countListingReservationsByStatus(long ownerId, long listingId);

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

    /**
     * Owner marks the vehicle returned for an in-progress rental; validates participant and state, may send mail.
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
     * Reservations in {@code pending}, {@code accepted}, or {@code started} for one listing (availability overlap checks).
     */
    List<Reservation> findBlockingReservationsByListingId(long listingId);

    /** Same as {@link #findBlockingReservationsByListingId(long)} for a batch of listings. */
    List<Reservation> findBlockingReservationsByListingIds(Collection<Long> listingIds);

    /**
     * Reservations whose pickup {@code start_date} lies in {@code [from, to)} (UTC), for the day-before reminder
     * scheduled job.
     */
    List<Reservation> findReminderReservations(OffsetDateTime from, OffsetDateTime to);
}
