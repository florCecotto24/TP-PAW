package ar.edu.itba.paw.services;

public class ReservationConflictException extends RuntimeException {

    public ReservationConflictException(final String message) {
        super(message);
    }
}

