package ar.edu.itba.paw.exception.user;

import ar.edu.itba.paw.exception.MessageKeys;

/**
 * Raised when an operation needs the user's CBU but none is stored on the profile. Part of the
 * {@code RydenException} family so the message resolves through the i18n exception bundle instead
 * of a hardcoded English string.
 */
public final class CBUNotFoundException extends UserException {

    private final long userId;

    public CBUNotFoundException(final long userId) {
        super(MessageKeys.USER_CBU_NOT_FOUND);
        this.userId = userId;
    }

    public long getUserId() {
        return userId;
    }
}
