package ar.edu.itba.paw.services;

public final class EmailMessagingException extends Exception {

    public EmailMessagingException(final String message) {
        super(message);
    }

    public EmailMessagingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public EmailMessagingException(final Throwable cause) {
        super(cause);
    }
}
