package ar.edu.itba.paw.services.reservation;


import java.util.Optional;

import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.reservation.ReservationParticipantRole;

/**
 * Mutating reservation lifecycle: rider submissions, rider-driven edits of unpaid pending
 * reservations, participant-driven and automatic cancellations, and owner confirmation that
 * the vehicle was returned. Pricing math, validations, and the timing-policy getters live in
 * {@link ReservationPricingService}; lookups in {@link ReservationQueryService}; receipt
 * uploads, refund tracking, and the payment-side scheduled jobs in
 * {@link ReservationPaymentService}.
 */
public interface ReservationWorkflowService {

    /**
     * Rider reservation from the web form: validate {@code riderId}, car/dates/availability,
     * then create the reservation.
     *
     * @throws ar.edu.itba.paw.exception.reservation.RiderReservationException for validation
     *         errors (message key in {@link ar.edu.itba.paw.exception.MessageKeys})
     * @throws ar.edu.itba.paw.exception.reservation.ReservationConflictException when the
     *         interval overlaps existing reservations
     */
    Reservation submitRiderReservationByCar(
            long riderId,
            long carId,
            Long availabilityId,
            String fromDateTime,
            String untilDateTime);

    /**
     * Rider-driven edit of the rental period of an unpaid pending reservation. Re-validates
     * the new window like a fresh submission and resets the payment-proof deadline relative
     * to the moment of the edit.
     */
    Reservation editPendingReservationByRider(
            long riderId,
            long reservationId,
            String fromDateTime,
            String untilDateTime);

    /**
     * Cancels without participant checks (e.g. expired pending payment job). Persists
     * {@link ar.edu.itba.paw.models.domain.reservation.Reservation.Status#CANCELLED_DUE_TO_MISSING_PAYMENT_PROOF}.
     */
    Optional<Reservation> cancelReservation(long reservationId);

    /**
     * Rider or owner may cancel {@code PENDING} without payment receipt, or either may cancel
     * {@code ACCEPTED} before pickup. Confirmed cancellations with a prior payment receipt
     * require the owner to upload a refund proof (handled via {@link ReservationPaymentService}).
     */
    Optional<Reservation> cancelReservationAsParticipant(long userId, long reservationId);

    /**
     * Role-scoped variant of {@link #cancelReservationAsParticipant(long, long)} that performs
     * the access pre-check internally and signals failure with typed domain exceptions instead
     * of {@link Optional}.
     *
     * @throws ar.edu.itba.paw.exception.reservation.ReservationAccessDeniedException when the
     *         reservation does not exist or the user is not a participant under {@code viewerRole}
     * @throws ar.edu.itba.paw.exception.reservation.ReservationCancelNotAllowedException when the
     *         reservation exists and the viewer has access but the state does not allow cancel
     */
    Reservation cancelReservationAsParticipantScoped(
            long viewerUserId, long reservationId, ReservationParticipantRole viewerRole);

    /**
     * Owner confirms vehicle returned after checkout; irreversible. When both return and
     * payment approval hold, {@code status} becomes {@code finished}, otherwise
     * {@code accepted}/{@code started}.
     */
    void markCarReturnedByOwner(long ownerUserId, long reservationId);
}
