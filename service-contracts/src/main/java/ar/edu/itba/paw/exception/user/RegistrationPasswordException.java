package ar.edu.itba.paw.exception.user;

public final class RegistrationPasswordException extends UserException {

    public RegistrationPasswordException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }
}
