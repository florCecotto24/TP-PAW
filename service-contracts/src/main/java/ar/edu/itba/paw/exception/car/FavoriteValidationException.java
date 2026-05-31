package ar.edu.itba.paw.exception.car;

import ar.edu.itba.paw.exception.RydenException;

/**
 * Thrown when a "favorite cars" toggle violates a business rule (e.g. a user trying to
 * favorite their own car, or favoriting a car that does not exist).
 */
public class FavoriteValidationException extends RydenException {

    public FavoriteValidationException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }
}
