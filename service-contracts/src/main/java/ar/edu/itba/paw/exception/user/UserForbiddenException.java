package ar.edu.itba.paw.exception.user;

import ar.edu.itba.paw.exception.MessageKeys;

/** Caller is authenticated but not allowed to perform the requested user mutation. */
public final class UserForbiddenException extends UserException {

    public UserForbiddenException() {
        super(MessageKeys.ERROR_FORBIDDEN);
    }
}
