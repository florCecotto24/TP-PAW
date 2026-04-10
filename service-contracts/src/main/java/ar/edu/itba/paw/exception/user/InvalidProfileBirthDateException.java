package ar.edu.itba.paw.exception.user;

public final class InvalidProfileBirthDateException extends UserException {

    public InvalidProfileBirthDateException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }
}
