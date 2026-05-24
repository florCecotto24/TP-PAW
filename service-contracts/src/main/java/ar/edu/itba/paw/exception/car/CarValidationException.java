package ar.edu.itba.paw.exception.car;

import ar.edu.itba.paw.exception.RydenException;

public class CarValidationException extends RydenException {

    public CarValidationException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }
}
