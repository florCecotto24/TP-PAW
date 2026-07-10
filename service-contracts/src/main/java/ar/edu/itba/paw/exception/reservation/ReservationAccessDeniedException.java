package ar.edu.itba.paw.exception.reservation;


import ar.edu.itba.paw.exception.MessageKeys;

/**
 * Thrown by participant-scoped reservation operations when the calling user does not have
 * access to the reservation under the requested viewer role (e.g. cancelling as "owner"
 * a reservation the user does not own, or whose row is missing for the given role).
 *
 * <p>Controllers should react with a generic redirect ({@code /my-reservations}); they
 * MUST NOT leak whether the reservation actually exists or is just inaccessible.</p>
 */
public final class ReservationAccessDeniedException extends ReservationException {

    private ReservationAccessDeniedException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }

    public static ReservationAccessDeniedException denied() {
        return new ReservationAccessDeniedException(MessageKeys.RESERVATION_CANCEL_NOT_ALLOWED);
    }

    public static ReservationAccessDeniedException withCode(final String messageCode, final Object... args) {
        return new ReservationAccessDeniedException(messageCode, args);
    }
}
