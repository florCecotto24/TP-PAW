package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.paw.models.Page;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.models.ReservationCard;
import ar.edu.itba.paw.models.StoredFile;

public interface ReservationService {

    Reservation createReservation(
            long riderId,
            long listingId,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Reservation.Status status,
            OffsetDateTime paymentProofDeadlineAt);

    Optional<Reservation> getReservationById(long id);

    Optional<Reservation> getRiderReservationById(long riderId, long reservationId);

    Optional<Reservation> getOwnerReservationById(long ownerId, long reservationId);

    Page<ReservationCard> getRiderReservationCards(long riderId, int page, int pageSize, String statusFilter);

    Page<ReservationCard> getOwnerReservationCards(long ownerId, int page, int pageSize, String statusFilter);

    Optional<String> normalizeClientReservationTotal(String reservationTotal);

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

    Optional<BigDecimal> calculateTotal(long listingId, OffsetDateTime startDate, OffsetDateTime endDate);

    long calculateBillableDays(OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Cancels without participant checks (e.g. expired pending payment job). Prefer
     * {@link #cancelReservationAsParticipant(long, long)} for user-initiated cancellations.
     */
    Optional<Reservation> cancelReservation(long reservationId);

    /**
     * Rider or owner may cancel {@code PENDING} while no payment receipt is attached, or either may cancel
     * {@code ACCEPTED}. Other states are rejected; participants are verified (rider/owner) before applying rules.
     */
    Optional<Reservation> cancelReservationAsParticipant(long userId, long reservationId);

    List<Reservation> getReservationsForCancellation(final long listingId);

    void cancelExpiredPendingPaymentReservations();

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

    Page<ReservationCard> getListingReservationCards(long ownerId, long listingId, int page, int pageSize, String statusFilter);

    Map<String, Long> countListingReservationsByStatus(long ownerId, long listingId);

    BigDecimal getListingTotalEarnings(long ownerId, long listingId);

    BigDecimal getListingPendingEarnings(long ownerId, long listingId);

    long getListingTotalDaysRented(long ownerId, long listingId);

    long getListingReservationsThisMonth(long ownerId, long listingId);

    Optional<OffsetDateTime> getListingNextReservationDate(long ownerId, long listingId);
}
