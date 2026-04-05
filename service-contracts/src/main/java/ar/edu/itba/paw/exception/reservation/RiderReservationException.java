package ar.edu.itba.paw.exception.reservation;

public final class RiderReservationException extends ReservationException {

    public RiderReservationException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }
}
