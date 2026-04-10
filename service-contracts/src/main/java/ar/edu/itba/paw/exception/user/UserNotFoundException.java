package ar.edu.itba.paw.exception.user;

public final class UserNotFoundException extends UserException {

    public UserNotFoundException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }
}
