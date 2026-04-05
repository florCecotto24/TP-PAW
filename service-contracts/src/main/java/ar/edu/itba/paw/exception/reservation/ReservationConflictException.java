package ar.edu.itba.paw.exception.reservation;

public final class ReservationConflictException extends ReservationException {

    public ReservationConflictException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }
}
