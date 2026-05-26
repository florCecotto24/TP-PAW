package ar.edu.itba.paw.exception.admin;

import ar.edu.itba.paw.exception.RydenException;

public abstract class AdminException extends RydenException {

    protected AdminException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }
}
