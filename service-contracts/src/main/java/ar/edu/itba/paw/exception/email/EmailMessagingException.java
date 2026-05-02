package ar.edu.itba.paw.exception.email;

/**
 * Checked exception for failures while composing or sending application email (JavaMail, MIME assembly, template rendering).
 */
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
