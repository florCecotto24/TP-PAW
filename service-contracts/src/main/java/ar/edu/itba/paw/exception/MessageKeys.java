package ar.edu.itba.paw.exception;

/**
 * Message keys for {@code messages/exception/exception-messages.properties} and locale files
 * ({@code messages/exception/exception-messages_es.properties}, empty {@code messages/exception/exception-messages_en.properties}). Keep in sync.
 * String constants are used (rather than a Java {@code enum}) so keys match the i18n bundle files directly; an
 * {@code enum} would add indirection and boilerplate here.
 */
public final class MessageKeys {

    private MessageKeys() {
    }

    // reservation
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
    public static final String RESERVATION_MARK_RETURNED_NOT_ALLOWED = "reservation.carReturned.notAllowed";
    public static final String RESERVATION_REFUND_RECEIPT_INVALID = "reservation.refundReceipt.invalid";
    public static final String RESERVATION_REFUND_RECEIPT_TOO_LARGE = "reservation.refundReceipt.tooLarge";
    public static final String RESERVATION_REFUND_APPROVAL_INVALID = "reservation.refundApproval.invalid";
    public static final String RESERVATION_CHAT_NOT_AVAILABLE = "reservation.chat.notAvailable";
    public static final String RESERVATION_CHAT_BODY_EMPTY = "reservation.chat.bodyEmpty";
    public static final String RESERVATION_CHAT_BODY_TOO_LONG = "reservation.chat.bodyTooLong";
    public static final String RESERVATION_CHAT_NOT_PARTICIPANT = "reservation.chat.notParticipant";
    public static final String RESERVATION_CHAT_ATTACHMENT_REQUIRED = "reservation.chat.attachmentRequired";
    public static final String RESERVATION_CHAT_ATTACHMENT_INVALID = "reservation.chat.attachmentInvalidType";
    public static final String RESERVATION_CHAT_ATTACHMENT_TOO_LARGE = "reservation.chat.attachmentTooLarge";
    public static final String RESERVATION_CHAT_ATTACHMENT_NOT_FOUND = "reservation.chat.attachmentNotFound";

    public static final String REVIEW_NOT_ALLOWED = "review.notAllowed";
    public static final String REVIEW_ALREADY_SUBMITTED = "review.alreadySubmitted";
    public static final String REVIEW_RATING_INVALID = "review.rating.invalid";
    public static final String REVIEW_RATING_REQUIRED_WHEN_COMMENT = "review.rating.requiredWhenComment";
    public static final String REVIEW_COMMENT_TOO_LONG = "review.comment.tooLong";
    public static final String RESERVATION_RIDER_OUTSIDE_AVAILABILITY = "reservation.rider.outsideAvailability";
    public static final String RESERVATION_RIDER_MAX_BILLABLE_DAYS = "reservation.rider.maxBillableDays";
    public static final String RESERVATION_RIDER_CANNOT_RESERVE_OWN_LISTING =
            "reservation.rider.cannotReserveOwnListing";
    public static final String RESERVATION_TOTAL_PRICE_INVALID = "reservation.totalPrice.invalid";
    public static final String RESERVATION_RIDER_AVAILABILITY_PERIOD_REQUIRED =
            "reservation.rider.availabilityPeriodRequired";
    public static final String RESERVATION_FORM_CAR_NAME_REQUIRED = "reservation.form.carNameRequired";
    public static final String RESERVATION_RIDER_USER_NOT_FOUND = "reservation.rider.userNotFound";
    public static final String RESERVATION_OWNER_PAYMENT_DETAILS_UNAVAILABLE = "reservation.rider.ownerPaymentDetailsUnavailable";
    public static final String RESERVATION_RIDER_DOCUMENTATION_REQUIRED = "reservation.rider.documentationRequired";

    // user
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
    public static final String USER_REGISTRATION_PASSWORD_TOO_LONG = "user.registration.passwordTooLong";
    public static final String USER_REGISTRATION_FORENAME_TOO_LONG = "user.registration.forenameTooLong";
    public static final String USER_REGISTRATION_SURNAME_TOO_LONG = "user.registration.surnameTooLong";
    public static final String USER_REGISTRATION_EMAIL_TOO_LONG = "user.registration.emailTooLong";
    public static final String USER_PROFILE_FORENAME_TOO_LONG = "user.profile.forenameTooLong";
    public static final String USER_PROFILE_SURNAME_TOO_LONG = "user.profile.surnameTooLong";
    public static final String USER_PROFILE_ABOUT_TOO_LONG = "user.profile.aboutTooLong";
    public static final String USER_PROFILE_DOCUMENT_INVALID = "user.profile.document.invalid";
    public static final String USER_PROFILE_DOCUMENT_TOO_LARGE = "user.profile.document.tooLarge";
    public static final String USER_PROFILE_DOCUMENT_ALREADY_UPLOADED = "user.profile.document.alreadyUploaded";
    public static final String USER_PROFILE_CBU_INVALID = "user.profile.cbuInvalid";

    // car
    public static final String CAR_PLATE_ALREADY_EXISTS = "car.plate.alreadyExists";

    // listing
    public static final String LISTING_AVAILABILITY_REQUIRED = "listing.availability.required";
    public static final String LISTING_AVAILABILITY_INVALID_ORDER = "listing.availability.invalidOrder";
    public static final String LISTING_AVAILABILITY_INCLUDES_PAST_DATES = "listing.availability.includesPastDates";
    public static final String LISTING_AVAILABILITY_BEYOND_PUBLISH_HORIZON = "listing.availability.beyondPublishHorizon";
    public static final String LISTING_CHECKOUT_NOT_AFTER_CHECKIN = "listing.times.checkOutNotAfterCheckIn";
    public static final String LISTING_CHECKINOUT_MIN_GAP = "listing.times.minGapHours";
    public static final String LISTING_CAR_NOT_FOUND = "listing.carNotFound";
    public static final String LISTING_CAR_NOT_OWNED = "listing.carNotOwned";
    public static final String LISTING_LIMIT_POSITIVE = "listing.limit.positive";
    public static final String LISTING_PICKUP_LOCATION_REQUIRED = "listing.pickupLocation.required";
    public static final String LISTING_PICKUP_STREET_NUMBER_DIGITS_ONLY = "listing.pickupStreetNumber.digitsOnly";
    public static final String LISTING_PICKUP_STREET_NUMBER_MAX_DIGITS = "listing.pickupStreetNumber.maxDigits";
    public static final String LISTING_PUBLISH_CBU_REQUIRED = "listing.publish.cbuRequired";
    public static final String LISTING_ACTIVATE_CBU_REQUIRED = "listing.activate.cbuRequired";
    // image
    public static final String IMAGE_INVALID_ID = "image.invalidId";
    public static final String IMAGE_FILE_TOO_LARGE = "image.file.tooLarge";
    public static final String IMAGE_CONTENT_TYPE_NOT_IMAGE = "image.contentType.notImage";

    // publish form
    public static final String PUBLISH_IMAGES_READ = "publish.images.read";
    public static final String PUBLISH_FAILED = "publish.failed";
}
