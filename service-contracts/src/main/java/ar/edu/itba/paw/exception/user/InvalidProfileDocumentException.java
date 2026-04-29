package ar.edu.itba.paw.exception.user;

public final class InvalidProfileDocumentException extends UserException {

    public InvalidProfileDocumentException(final String messageCode, final Object... args) {
        super(messageCode, args);
    }
}
