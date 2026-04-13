package ar.edu.itba.paw.exception.user;

public final class PasswordResetCodeInvalidException extends UserException {

    public PasswordResetCodeInvalidException(final String messageCode) {
        super(messageCode);
    }
}
