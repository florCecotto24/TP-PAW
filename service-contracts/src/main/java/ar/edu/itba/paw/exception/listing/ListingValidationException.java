package ar.edu.itba.paw.exception.listing;

import ar.edu.itba.paw.exception.RydenException;

public class ListingValidationException extends RydenException {

    public ListingValidationException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }
}
