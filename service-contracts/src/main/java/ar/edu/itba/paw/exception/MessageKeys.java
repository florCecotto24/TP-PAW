package ar.edu.itba.paw.exception;

/**
 * Keys for {@code exception-messages.properties} (and locale variants). Keep in sync with that file.
 */
public final class MessageKeys {

    private MessageKeys() {
    }

    /* reservation */
    public static final String RESERVATION_CONFLICT_OVERLAP = "reservation.conflict.overlap";
    public static final String RESERVATION_RIDER_LISTING_NOT_FOUND = "reservation.rider.listingNotFound";
    public static final String RESERVATION_RIDER_DATES_REQUIRED = "reservation.rider.datesRequired";
    public static final String RESERVATION_RIDER_DATES_INVALID_FORMAT = "reservation.rider.datesInvalidFormat";
    public static final String RESERVATION_RIDER_END_NOT_AFTER_START = "reservation.rider.endNotAfterStart";
    public static final String RESERVATION_RIDER_DATES_NOT_FROM_TODAY = "reservation.rider.datesNotFromToday";
    public static final String RESERVATION_RIDER_PICKUP_MIN_24H = "reservation.rider.pickupMin24h";
    public static final String RESERVATION_PAYMENT_RECEIPT_INVALID = "reservation.paymentReceipt.invalid";
    public static final String RESERVATION_PAYMENT_RECEIPT_TOO_LARGE = "reservation.paymentReceipt.tooLarge";
    public static final String RESERVATION_PAYMENT_RECEIPT_NOT_FOUND = "reservation.paymentReceipt.notFound";
    public static final String RESERVATION_PAYMENT_APPROVAL_INVALID = "reservation.paymentApproval.invalid";
    public static final String RESERVATION_CANCEL_NOT_ALLOWED = "reservation.cancel.notAllowed";
    public static final String RESERVATION_RIDER_OUTSIDE_AVAILABILITY = "reservation.rider.outsideAvailability";
    public static final String RESERVATION_RIDER_CANNOT_RESERVE_OWN_LISTING =
            "reservation.rider.cannotReserveOwnListing";
    public static final String RESERVATION_TOTAL_PRICE_INVALID = "reservation.totalPrice.invalid";
    public static final String RESERVATION_RIDER_AVAILABILITY_PERIOD_REQUIRED =
            "reservation.rider.availabilityPeriodRequired";
    public static final String RESERVATION_FORM_CAR_NAME_REQUIRED = "reservation.form.carNameRequired";
    public static final String RESERVATION_RIDER_USER_NOT_FOUND = "reservation.rider.userNotFound";

    /* user */
    public static final String USER_EMAIL_ALREADY_EXISTS = "user.email.alreadyExists";
    public static final String USER_ACCOUNT_NOT_FOUND = "user.account.notFound";
    public static final String USER_PROFILE_PHONE_INVALID = "user.profile.phoneInvalid";
    public static final String USER_PROFILE_BIRTH_DATE_FUTURE = "user.profile.birthDateFuture";
    public static final String USER_VERIFICATION_CODE_INVALID = "user.verification.codeInvalid";
    public static final String USER_VERIFICATION_CODE_ALREADY_ACTIVE = "user.verification.codeAlreadyActive";
    public static final String USER_PASSWORD_RESET_CODE_INVALID = "user.passwordReset.codeInvalid";
    public static final String USER_PASSWORD_RESET_CODE_ALREADY_ACTIVE = "user.passwordReset.codeAlreadyActive";
    public static final String USER_PASSWORD_CURRENT_INCORRECT = "user.password.currentIncorrect";
    public static final String USER_REGISTRATION_PASSWORD_MISMATCH = "user.registration.passwordMismatch";
    public static final String USER_REGISTRATION_PASSWORD_TOO_SHORT = "user.registration.passwordTooShort";

    /* listing */
    public static final String LISTING_AVAILABILITY_REQUIRED = "listing.availability.required";
    public static final String LISTING_AVAILABILITY_INVALID_ORDER = "listing.availability.invalidOrder";
    public static final String LISTING_AVAILABILITY_INCLUDES_PAST_DATES = "listing.availability.includesPastDates";
    public static final String LISTING_CHECKOUT_NOT_AFTER_CHECKIN = "listing.times.checkOutNotAfterCheckIn";
    public static final String LISTING_CAR_NOT_FOUND = "listing.carNotFound";
    public static final String LISTING_LIMIT_POSITIVE = "listing.limit.positive";
    public static final String LISTING_PICKUP_LOCATION_REQUIRED = "listing.pickupLocation.required";
    public static final String LISTING_PICKUP_STREET_NUMBER_DIGITS_ONLY = "listing.pickupStreetNumber.digitsOnly";
    public static final String LISTING_PICKUP_STREET_NUMBER_MAX_DIGITS = "listing.pickupStreetNumber.maxDigits";
    /* image */
    public static final String IMAGE_INVALID_ID = "image.invalidId";
    public static final String IMAGE_FILE_TOO_LARGE = "image.file.tooLarge";
    public static final String IMAGE_CONTENT_TYPE_NOT_IMAGE = "image.contentType.notImage";

    /* publish form */
    public static final String PUBLISH_IMAGES_READ = "publish.images.read";
    public static final String PUBLISH_FAILED = "publish.failed";
}
