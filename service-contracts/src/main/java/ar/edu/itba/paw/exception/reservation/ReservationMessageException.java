package ar.edu.itba.paw.exception.reservation;

public final class ReservationMessageException extends ReservationException {

    public ReservationMessageException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }
}
