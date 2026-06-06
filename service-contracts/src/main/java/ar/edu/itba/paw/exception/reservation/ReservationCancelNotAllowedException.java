package ar.edu.itba.paw.exception.reservation;


import ar.edu.itba.paw.exception.MessageKeys;

/**
 * Thrown by {@code ReservationService.cancelReservationAsParticipantScoped} when the
 * reservation is visible to the caller in the given viewer role but its current state
 * does not allow self-cancellation (e.g. already cancelled, completed, or otherwise
 * outside the rider/owner-cancellable window).
 *
 * <p>Controllers should surface the localised message via flash attribute and redirect
 * back to the reservation detail page — the user still has access to the reservation.</p>
 */
public final class ReservationCancelNotAllowedException extends ReservationException {

    public ReservationCancelNotAllowedException() {
        super(MessageKeys.RESERVATION_CANCEL_NOT_ALLOWED);
    }

    public ReservationCancelNotAllowedException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }
}
