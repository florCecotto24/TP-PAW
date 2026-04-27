package ar.edu.itba.paw.exception.user;

public final class InvalidUserFieldLengthException extends UserException {

    public InvalidUserFieldLengthException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }
}
