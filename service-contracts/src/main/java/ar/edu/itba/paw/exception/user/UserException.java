package ar.edu.itba.paw.exception.user;

import ar.edu.itba.paw.exception.RydenException;

public abstract class UserException extends RydenException {

    protected UserException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }
}
