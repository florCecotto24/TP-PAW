package ar.edu.itba.paw.exception.user;

public final class InvalidProfilePhoneException extends UserException {

    public InvalidProfilePhoneException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }
}
