package ar.edu.itba.paw.exception.user;

public final class IncorrectCurrentPasswordException extends UserException {

    public IncorrectCurrentPasswordException(final String messageCode) {
        super(messageCode);
    }
}
