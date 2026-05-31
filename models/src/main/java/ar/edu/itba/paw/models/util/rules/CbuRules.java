package ar.edu.itba.paw.models.util.rules;

/**
 * Central definition of CBU format for payouts. If the banking standard changes length, update {@link #REQUIRED_DIGIT_LENGTH} once.
 */
public final class CbuRules {

    /** Expected number of digits after trimming spaces (Argentina CBU as of integration date). */
    public static final int REQUIRED_DIGIT_LENGTH = 22;

    private static final String DIGITS_ONLY_PATTERN = "\\d{" + REQUIRED_DIGIT_LENGTH + "}";

    private CbuRules() {
    }

    /**
     * @return {@code true} when {@code cbuRaw} is non-null and its trim is exactly {@link #REQUIRED_DIGIT_LENGTH} decimal digits.
     */
    public static boolean isValidFormat(final String cbuRaw) {
        if (cbuRaw == null) {
            return false;
        }
        final String t = cbuRaw.trim();
        return t.matches(DIGITS_ONLY_PATTERN);
    }
}
