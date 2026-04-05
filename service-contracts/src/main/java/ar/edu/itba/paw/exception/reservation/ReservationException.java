package ar.edu.itba.paw.exception.reservation;

import ar.edu.itba.paw.exception.RydenException;

public abstract class ReservationException extends RydenException {

    protected ReservationException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }
}
