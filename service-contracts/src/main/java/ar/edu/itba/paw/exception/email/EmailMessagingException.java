package ar.edu.itba.paw.exception.email;

/**
 * Checked exception for failures while composing or sending application email (JavaMail, MIME assembly, template rendering).
 */
public final class EmailMessagingException extends Exception {

    private EmailMessagingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public static EmailMessagingException withMessage(final String message) {
        return new EmailMessagingException(message, null);
    }

    public static EmailMessagingException withMessageAndCause(final String message, final Throwable cause) {
        return new EmailMessagingException(message, cause);
    }

    public static EmailMessagingException wrapping(final Throwable cause) {
        return new EmailMessagingException(cause != null ? cause.getMessage() : null, cause);
    }
}
