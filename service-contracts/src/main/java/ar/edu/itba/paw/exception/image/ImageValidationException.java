package ar.edu.itba.paw.exception.image;

import ar.edu.itba.paw.exception.RydenException;

public final class ImageValidationException extends RydenException {

    public ImageValidationException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }
}
