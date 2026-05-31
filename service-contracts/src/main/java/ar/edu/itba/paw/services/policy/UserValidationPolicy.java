package ar.edu.itba.paw.services.policy;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Central limits for user account and profile text, aligned with {@code app.validation.*} (excluding review comments).
 * Instances are obtained via {@link #fromValidatedConfiguration(int, int, int, int, int, int, String)} so invariants are
 * checked at creation time.
 */
public final class UserValidationPolicy {

    private final int registrationPasswordMinLength;
    private final int registrationPasswordMaxLength;
    private final int registrationEmailMaxLength;
    private final int displayNamePartMaxLength;
    private final int profilePhoneMaxLength;
    private final int profileAboutMaxLength;
    private final Pattern profilePhonePattern;

    /**
     * Validates already-resolved primitive values (e.g. from application properties) and builds the policy.
     */
    public static UserValidationPolicy fromValidatedConfiguration(
            final int registrationPasswordMinLength,
            final int registrationPasswordMaxLength,
            final int registrationEmailMaxLength,
            final int displayNamePartMaxLength,
            final int profilePhoneMaxLength,
            final int profileAboutMaxLength,
            final String profilePhonePatternRaw) {
        requireAtLeast("app.validation.registration-password-min-length", registrationPasswordMinLength, 1);
        if (registrationPasswordMaxLength < registrationPasswordMinLength) {
            throw new IllegalArgumentException(
                    "app.validation.registration-password-max-length must be >= min length, got "
                            + registrationPasswordMaxLength + " < " + registrationPasswordMinLength);
        }
        requireAtLeast("app.validation.registration-email-max-length", registrationEmailMaxLength, 1);
        requireAtLeast("app.validation.display-name-part-max-length", displayNamePartMaxLength, 1);
        requireAtLeast("app.validation.profile-phone-max-length", profilePhoneMaxLength, 1);
        requireAtLeast("app.validation.profile-about-max-length", profileAboutMaxLength, 1);
        final Pattern compiled = compileProfilePhonePattern(profilePhonePatternRaw);
        return new UserValidationPolicy(
                registrationPasswordMinLength,
                registrationPasswordMaxLength,
                registrationEmailMaxLength,
                displayNamePartMaxLength,
                profilePhoneMaxLength,
                profileAboutMaxLength,
                compiled);
    }

    private static Pattern compileProfilePhonePattern(final String pattern) {
        try {
            return Pattern.compile(pattern);
        } catch (final PatternSyntaxException e) {
            throw new IllegalArgumentException("app.validation.profile-phone-pattern is not a valid regex: " + pattern, e);
        }
    }

    private static void requireAtLeast(final String propertyKey, final int value, final int minInclusive) {
        if (value < minInclusive) {
            throw new IllegalArgumentException(propertyKey + " must be >= " + minInclusive + ", got " + value);
        }
    }

    /** Canonical constructor; use {@link #fromValidatedConfiguration(int, int, int, int, int, int, String)} from outside. */
    private UserValidationPolicy(
            final int registrationPasswordMinLength,
            final int registrationPasswordMaxLength,
            final int registrationEmailMaxLength,
            final int displayNamePartMaxLength,
            final int profilePhoneMaxLength,
            final int profileAboutMaxLength,
            final Pattern profilePhonePattern) {
        this.registrationPasswordMinLength = registrationPasswordMinLength;
        this.registrationPasswordMaxLength = registrationPasswordMaxLength;
        this.registrationEmailMaxLength = registrationEmailMaxLength;
        this.displayNamePartMaxLength = displayNamePartMaxLength;
        this.profilePhoneMaxLength = profilePhoneMaxLength;
        this.profileAboutMaxLength = profileAboutMaxLength;
        this.profilePhonePattern = profilePhonePattern;
    }

    public int getRegistrationPasswordMinLength() {
        return registrationPasswordMinLength;
    }

    public int getRegistrationPasswordMaxLength() {
        return registrationPasswordMaxLength;
    }

    public int getRegistrationEmailMaxLength() {
        return registrationEmailMaxLength;
    }

    public int getDisplayNamePartMaxLength() {
        return displayNamePartMaxLength;
    }

    public int getProfilePhoneMaxLength() {
        return profilePhoneMaxLength;
    }

    public int getProfileAboutMaxLength() {
        return profileAboutMaxLength;
    }

    public Pattern getProfilePhonePattern() {
        return profilePhonePattern;
    }
}
