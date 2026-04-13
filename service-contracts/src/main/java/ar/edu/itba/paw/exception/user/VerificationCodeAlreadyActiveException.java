package ar.edu.itba.paw.exception.user;

public final class VerificationCodeAlreadyActiveException extends UserException {

    public VerificationCodeAlreadyActiveException(final String messageCode) {
        super(messageCode);
    }
}
