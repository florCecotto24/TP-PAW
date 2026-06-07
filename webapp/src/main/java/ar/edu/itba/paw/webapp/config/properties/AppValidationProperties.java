package ar.edu.itba.paw.webapp.config.properties;

import java.math.BigDecimal;

import org.springframework.core.env.Environment;

import ar.edu.itba.paw.policy.CarValidationPolicy;
import ar.edu.itba.paw.policy.ListingFormValidationPolicy;
import ar.edu.itba.paw.policy.ReservationFormValidationPolicy;
import ar.edu.itba.paw.policy.ReservationMessageValidationPolicy;
import ar.edu.itba.paw.policy.ReviewValidationPolicy;
import ar.edu.itba.paw.policy.UserValidationPolicy;
import ar.edu.itba.paw.policy.VerificationCodePolicy;

/**
 * Bound view of {@code app.validation.*}. Add new keys and defaults here, then expose them via accessors or
 * a {@code to*Policy()} factory (validation stays in the policy factories themselves).
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
        int reservationMessageMaxLength,
        int carBrandMinLength,
        int carBrandMaxLength,
        int carModelMaxLength,
        int carPlateMinLength,
        int carPlateMaxLength,
        int carDescriptionMaxLength,
        int carYearMin,
        int listingAddressStreetMaxLength,
        int listingAddressNumberMaxLength,
        int listingAvailabilityRowsMin,
        int listingAvailabilityRowsMax,
        int listingMinimumRentalDaysMin,
        int listingMinimumRentalDaysMax,
        BigDecimal listingPricePerDayMin,
        int listingPricePerDayIntegerDigits,
        int listingPricePerDayFractionDigits,
        int reservationDeliveryLocationMaxLength,
        int reservationCarNameMaxLength,
        int reservationDatetimeInputMaxLength,
        int verificationCodeLength) {

    private static final String REGISTRATION_PASSWORD_MIN_LENGTH = "app.validation.registration-password-min-length";
    private static final String REGISTRATION_PASSWORD_MAX_LENGTH = "app.validation.registration-password-max-length";
    private static final String REGISTRATION_EMAIL_MAX_LENGTH = "app.validation.registration-email-max-length";
    private static final String DISPLAY_NAME_PART_MAX_LENGTH = "app.validation.display-name-part-max-length";
    private static final String PROFILE_PHONE_MAX_LENGTH = "app.validation.profile-phone-max-length";
    private static final String PROFILE_ABOUT_MAX_LENGTH = "app.validation.profile-about-max-length";
    private static final String PROFILE_PHONE_PATTERN = "app.validation.profile-phone-pattern";
    private static final String REVIEW_COMMENT_MAX_LENGTH = "app.validation.review-comment-max-length";
    private static final String RESERVATION_MESSAGE_MAX_LENGTH = "app.validation.reservation-message-max-length";

    private static final String CAR_BRAND_MIN_LENGTH = "app.validation.car-brand-min-length";
    private static final String CAR_BRAND_MAX_LENGTH = "app.validation.car-brand-max-length";
    private static final String CAR_MODEL_MAX_LENGTH = "app.validation.car-model-max-length";
    private static final String CAR_PLATE_MIN_LENGTH = "app.validation.car-plate-min-length";
    private static final String CAR_PLATE_MAX_LENGTH = "app.validation.car-plate-max-length";
    private static final String CAR_DESCRIPTION_MAX_LENGTH = "app.validation.car-description-max-length";
    private static final String CAR_YEAR_MIN = "app.validation.car-year-min";

    private static final String LISTING_ADDRESS_STREET_MAX_LENGTH = "app.validation.listing-address-street-max-length";
    private static final String LISTING_ADDRESS_NUMBER_MAX_LENGTH = "app.validation.listing-address-number-max-length";
    private static final String LISTING_AVAILABILITY_ROWS_MIN = "app.validation.listing-availability-rows-min";
    private static final String LISTING_AVAILABILITY_ROWS_MAX = "app.validation.listing-availability-rows-max";
    private static final String LISTING_MINIMUM_RENTAL_DAYS_MIN = "app.validation.listing-minimum-rental-days-min";
    private static final String LISTING_MINIMUM_RENTAL_DAYS_MAX = "app.validation.listing-minimum-rental-days-max";
    private static final String LISTING_PRICE_PER_DAY_MIN = "app.validation.listing-price-per-day-min";
    private static final String LISTING_PRICE_PER_DAY_INTEGER_DIGITS = "app.validation.listing-price-per-day-integer-digits";
    private static final String LISTING_PRICE_PER_DAY_FRACTION_DIGITS = "app.validation.listing-price-per-day-fraction-digits";

    private static final String RESERVATION_DELIVERY_LOCATION_MAX_LENGTH =
            "app.validation.reservation-delivery-location-max-length";
    private static final String RESERVATION_CAR_NAME_MAX_LENGTH = "app.validation.reservation-car-name-max-length";
    private static final String RESERVATION_DATETIME_INPUT_MAX_LENGTH =
            "app.validation.reservation-datetime-input-max-length";

    private static final String VERIFICATION_CODE_LENGTH = "app.validation.verification-code-length";

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
                environment.getProperty(RESERVATION_MESSAGE_MAX_LENGTH, Integer.class, 1000),
                environment.getProperty(CAR_BRAND_MIN_LENGTH, Integer.class, 2),
                environment.getProperty(CAR_BRAND_MAX_LENGTH, Integer.class, 50),
                environment.getProperty(CAR_MODEL_MAX_LENGTH, Integer.class, 50),
                environment.getProperty(CAR_PLATE_MIN_LENGTH, Integer.class, 6),
                environment.getProperty(CAR_PLATE_MAX_LENGTH, Integer.class, 10),
                environment.getProperty(CAR_DESCRIPTION_MAX_LENGTH, Integer.class, 200),
                environment.getProperty(CAR_YEAR_MIN, Integer.class, 1886),
                environment.getProperty(LISTING_ADDRESS_STREET_MAX_LENGTH, Integer.class, 250),
                environment.getProperty(LISTING_ADDRESS_NUMBER_MAX_LENGTH, Integer.class, 10),
                environment.getProperty(LISTING_AVAILABILITY_ROWS_MIN, Integer.class, 1),
                environment.getProperty(LISTING_AVAILABILITY_ROWS_MAX, Integer.class, 10),
                environment.getProperty(LISTING_MINIMUM_RENTAL_DAYS_MIN, Integer.class, 1),
                environment.getProperty(LISTING_MINIMUM_RENTAL_DAYS_MAX, Integer.class, 365),
                environment.getProperty(LISTING_PRICE_PER_DAY_MIN, BigDecimal.class, new BigDecimal("0.01")),
                environment.getProperty(LISTING_PRICE_PER_DAY_INTEGER_DIGITS, Integer.class, 8),
                environment.getProperty(LISTING_PRICE_PER_DAY_FRACTION_DIGITS, Integer.class, 2),
                environment.getProperty(RESERVATION_DELIVERY_LOCATION_MAX_LENGTH, Integer.class, 250),
                environment.getProperty(RESERVATION_CAR_NAME_MAX_LENGTH, Integer.class, 120),
                environment.getProperty(RESERVATION_DATETIME_INPUT_MAX_LENGTH, Integer.class, 40),
                environment.getProperty(VERIFICATION_CODE_LENGTH, Integer.class, 6));
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

    public CarValidationPolicy toCarValidationPolicy() {
        return CarValidationPolicy.fromValidatedConfiguration(
                carBrandMinLength,
                carBrandMaxLength,
                carModelMaxLength,
                carPlateMinLength,
                carPlateMaxLength,
                carDescriptionMaxLength,
                carYearMin);
    }

    public ListingFormValidationPolicy toListingFormValidationPolicy() {
        return ListingFormValidationPolicy.fromValidatedConfiguration(
                listingAddressStreetMaxLength,
                listingAddressNumberMaxLength,
                listingAvailabilityRowsMin,
                listingAvailabilityRowsMax,
                listingMinimumRentalDaysMin,
                listingMinimumRentalDaysMax,
                listingPricePerDayMin,
                listingPricePerDayIntegerDigits,
                listingPricePerDayFractionDigits);
    }

    public ReservationFormValidationPolicy toReservationFormValidationPolicy() {
        return ReservationFormValidationPolicy.fromValidatedConfiguration(
                reservationDeliveryLocationMaxLength,
                reservationCarNameMaxLength,
                reservationDatetimeInputMaxLength);
    }

    public VerificationCodePolicy toVerificationCodePolicy() {
        return VerificationCodePolicy.fromValidatedCodeLength(verificationCodeLength);
    }
}
