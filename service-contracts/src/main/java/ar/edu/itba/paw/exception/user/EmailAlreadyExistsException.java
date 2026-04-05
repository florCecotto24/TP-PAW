package ar.edu.itba.paw.exception.user;

public final class EmailAlreadyExistsException extends UserException {

    public EmailAlreadyExistsException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }
}
