package ar.edu.itba.paw.webapp.config.properties;

import org.springframework.core.env.Environment;

import ar.edu.itba.paw.services.policy.ReservationMessageValidationPolicy;
import ar.edu.itba.paw.services.policy.ReviewValidationPolicy;
import ar.edu.itba.paw.services.policy.UserValidationPolicy;

/**
 * Bound view of {@code app.validation.*}. Add new keys and defaults here, then expose them via accessors or
 * {@link #toUserValidationPolicy()} / {@link #toReviewValidationPolicy()} (validation stays in the policy factories).
 */
public record AppValidationProperties(
        int registrationPasswordMinLength,
        int registrationPasswordMaxLength,
        int registrationEmailMaxLength,
        int displayNamePartMaxLength,
        int profilePhoneMaxLength,
        int profileAboutMaxLength,
        String profilePhonePattern,
        int reviewCommentMaxLength,
        int reservationMessageMaxLength) {

    private static final String REGISTRATION_PASSWORD_MIN_LENGTH = "app.validation.registration-password-min-length";
    private static final String REGISTRATION_PASSWORD_MAX_LENGTH = "app.validation.registration-password-max-length";
    private static final String REGISTRATION_EMAIL_MAX_LENGTH = "app.validation.registration-email-max-length";
    private static final String DISPLAY_NAME_PART_MAX_LENGTH = "app.validation.display-name-part-max-length";
    private static final String PROFILE_PHONE_MAX_LENGTH = "app.validation.profile-phone-max-length";
    private static final String PROFILE_ABOUT_MAX_LENGTH = "app.validation.profile-about-max-length";
    private static final String PROFILE_PHONE_PATTERN = "app.validation.profile-phone-pattern";
    private static final String REVIEW_COMMENT_MAX_LENGTH = "app.validation.review-comment-max-length";
    private static final String RESERVATION_MESSAGE_MAX_LENGTH = "app.validation.reservation-message-max-length";

    public static AppValidationProperties fromEnvironment(final Environment environment) {
        return new AppValidationProperties(
                environment.getProperty(REGISTRATION_PASSWORD_MIN_LENGTH, Integer.class, 8),
                environment.getProperty(REGISTRATION_PASSWORD_MAX_LENGTH, Integer.class, 72),
                environment.getProperty(REGISTRATION_EMAIL_MAX_LENGTH, Integer.class, 50),
                environment.getProperty(DISPLAY_NAME_PART_MAX_LENGTH, Integer.class, 50),
                environment.getProperty(PROFILE_PHONE_MAX_LENGTH, Integer.class, 20),
                environment.getProperty(PROFILE_ABOUT_MAX_LENGTH, Integer.class, 500),
                environment.getProperty(PROFILE_PHONE_PATTERN, "^[0-9+]+$"),
                environment.getProperty(REVIEW_COMMENT_MAX_LENGTH, Integer.class, 200),
                environment.getProperty(RESERVATION_MESSAGE_MAX_LENGTH, Integer.class, 1000));
    }

    public UserValidationPolicy toUserValidationPolicy() {
        return UserValidationPolicy.fromValidatedConfiguration(
                registrationPasswordMinLength,
                registrationPasswordMaxLength,
                registrationEmailMaxLength,
                displayNamePartMaxLength,
                profilePhoneMaxLength,
                profileAboutMaxLength,
                profilePhonePattern);
    }

    public ReviewValidationPolicy toReviewValidationPolicy() {
        return ReviewValidationPolicy.fromValidatedCommentMaxLength(reviewCommentMaxLength);
    }

    public ReservationMessageValidationPolicy toReservationMessageValidationPolicy() {
        return ReservationMessageValidationPolicy.fromValidatedBodyMaxLength(reservationMessageMaxLength);
    }
}
