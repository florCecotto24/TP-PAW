package ar.edu.itba.paw.exception.user;

public final class VerificationCodeInvalidException extends UserException {

    public VerificationCodeInvalidException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }
}
