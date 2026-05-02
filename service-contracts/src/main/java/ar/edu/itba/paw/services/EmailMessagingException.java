package ar.edu.itba.paw.services;

/** Checked wrapper for failures while building or sending mail (JavaMail, template processing). */
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
