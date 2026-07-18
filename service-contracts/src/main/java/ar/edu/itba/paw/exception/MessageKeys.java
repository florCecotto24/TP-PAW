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
    public static final String RESERVATION_PAYMENT_PROOF_DEADLINE_PASSED = "reservation.paymentProof.deadlinePassed";
    public static final String RESERVATION_PAYMENT_RECEIPT_TOO_LARGE = "reservation.paymentReceipt.tooLarge";
    public static final String RESERVATION_PAYMENT_RECEIPT_NOT_FOUND = "reservation.paymentReceipt.notFound";
    public static final String RESERVATION_CANCEL_NOT_ALLOWED = "reservation.cancel.notAllowed";
    public static final String RESERVATION_EDIT_NOT_ALLOWED = "reservation.edit.notAllowed";
    public static final String RESERVATION_MARK_RETURNED_NOT_ALLOWED = "reservation.carReturned.notAllowed";
    public static final String RESERVATION_REFUND_RECEIPT_INVALID = "reservation.refundReceipt.invalid";
    public static final String RESERVATION_REFUND_RECEIPT_TOO_LARGE = "reservation.refundReceipt.tooLarge";
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
    public static final String RESERVATION_RIDER_HANDOVER_TIME_MISMATCH =
            "reservation.rider.handoverTimeMismatch";
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
    public static final String RESERVATION_OWNER_BLOCKED = "reservation.rider.ownerBlocked";
    public static final String RESERVATION_RIDER_BLOCKED = "reservation.rider.riderBlocked";
    public static final String RESERVATION_CAR_NOT_ACTIVE = "reservation.rider.carNotActive";

    // user
    public static final String USER_EMAIL_ALREADY_EXISTS = "user.email.alreadyExists";
    public static final String USER_ACCOUNT_NOT_FOUND = "user.account.notFound";
    public static final String USER_PROFILE_PHONE_INVALID = "user.profile.phoneInvalid";
    public static final String USER_PROFILE_BIRTH_DATE_FUTURE = "user.profile.birthDateFuture";
    public static final String USER_VERIFICATION_CODE_INVALID = "user.verification.codeInvalid";
    public static final String USER_VERIFICATION_CODE_ALREADY_ACTIVE = "user.verification.codeAlreadyActive";
    public static final String USER_OTP_ATTEMPTS_EXCEEDED = "user.otp.attemptsExceeded";
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
    public static final String CAR_NOT_FOUND = "car.notFound";
    public static final String CAR_INVALID_STATUS_TRANSITION = "car.invalidStatusTransition";
    public static final String CAR_INSURANCE_INVALID = "car.insurance.invalid";
    public static final String CAR_INSURANCE_TOO_LARGE = "car.insurance.tooLarge";

    // favorite cars
    public static final String FAV_CAR_NOT_FOUND = "favCar.notFound";
    public static final String FAV_CAR_CANNOT_FAV_OWN = "favCar.cannotFavOwn";

    // car availability (period of offering for a car)
    public static final String CAR_AVAILABILITY_REQUIRED = "carAvailability.required";
    public static final String CAR_AVAILABILITY_INVALID_ORDER = "carAvailability.invalidOrder";
    public static final String CAR_AVAILABILITY_INCLUDES_PAST_DATES = "carAvailability.includesPastDates";
    public static final String CAR_AVAILABILITY_BEYOND_PUBLISH_HORIZON = "carAvailability.beyondPublishHorizon";
    public static final String CAR_AVAILABILITY_EDIT_CONFLICT = "carAvailability.editConflict";
    public static final String CAR_AVAILABILITY_WITHDRAW_CONFLICT = "carAvailability.withdrawConflict";
    public static final String CAR_AVAILABILITY_PUBLISH_CONFLICT = "carAvailability.publishConflict";
    public static final String CAR_AVAILABILITY_NOT_FOUND = "carAvailability.notFound";
    public static final String CAR_AVAILABILITY_NOT_OWNED = "carAvailability.notOwned";
    public static final String CAR_AVAILABILITY_NOT_OFFERED = "carAvailability.notOffered";
    public static final String CAR_AVAILABILITY_CHECKOUT_NOT_AFTER_CHECKIN = "carAvailability.times.checkOutNotAfterCheckIn";
    public static final String CAR_AVAILABILITY_CHECKINOUT_MIN_GAP = "carAvailability.times.minGapHours";
    public static final String PAGINATION_LIMIT_POSITIVE = "pagination.limit.positive";
    public static final String CAR_AVAILABILITY_PICKUP_LOCATION_REQUIRED = "carAvailability.pickupLocation.required";
    public static final String CAR_AVAILABILITY_PICKUP_STREET_NUMBER_DIGITS_ONLY = "carAvailability.pickupStreetNumber.digitsOnly";
    public static final String CAR_AVAILABILITY_PICKUP_STREET_NUMBER_MAX_DIGITS = "carAvailability.pickupStreetNumber.maxDigits";
    public static final String CAR_PUBLISH_CBU_REQUIRED = "car.publish.cbuRequired";
    public static final String CAR_ACTIVATE_CBU_REQUIRED = "car.activate.cbuRequired";
    public static final String CAR_ACTIVATE_IDENTITY_REQUIRED = "car.activate.identityRequired";
    public static final String CAR_ACTIVATE_OWNER_BLOCKED = "car.activate.ownerBlocked";
    public static final String CAR_CREATE_MODEL_PENDING = "car.create.modelPending";
    // Owner-blocked guards for owner-side mutations that could re-introduce bookability.
    // Reused across publishCar, createCarAvailabilityPeriods, applyOwnerEditByCar, and insurance upload —
    // the message body is the same ("upload the missing refund proof to unblock") regardless of entry point.
    public static final String CAR_MUTATION_OWNER_BLOCKED = "car.mutation.ownerBlocked";
    // image
    public static final String IMAGE_INVALID_ID = "image.invalidId";
    public static final String IMAGE_FILE_TOO_LARGE = "image.file.tooLarge";
    public static final String CAR_GALLERY_MEDIA_INVALID_TYPE = "car.gallery.media.invalidType";
    public static final String CAR_GALLERY_PICTURES_REQUIRED = "car.gallery.pictures.required";
    public static final String CAR_GALLERY_VIDEO_TOO_LARGE = "car.gallery.video.tooLarge";
    public static final String IMAGE_CONTENT_TYPE_NOT_IMAGE = "image.contentType.notImage";

    // publish form
    public static final String PUBLISH_IMAGES_READ = "publish.images.read";
    public static final String PUBLISH_FAILED = "publish.failed";
    public static final String PUBLISH_PREREQUISITES_MISSING = "publish.prerequisites.missing";

    // admin
    public static final String ADMIN_BLOCK_CANNOT_BLOCK_GRANTOR = "admin.block.cannotBlockGrantor";
    public static final String ADMIN_BLOCK_CANNOT_BLOCK_SELF = "admin.block.cannotBlockSelf";
    public static final String ADMIN_PROMOTE_NOT_ADMIN = "admin.promote.notAdmin";
    public static final String ADMIN_PROMOTE_ALREADY_ADMIN = "admin.promote.alreadyAdmin";
    public static final String ADMIN_PAUSE_CANNOT_PAUSE_ADMIN_CAR = "admin.pause.cannotPauseAdminCar";

    // catalog
    public static final String CATALOG_BRAND_NOT_FOUND = "catalog.brand.notFound";
    public static final String CATALOG_MODEL_NOT_FOUND = "catalog.model.notFound";
    public static final String CATALOG_BRAND_ALREADY_EXISTS = "catalog.brand.alreadyExists";
    public static final String CATALOG_MODEL_ALREADY_EXISTS = "catalog.model.alreadyExists";

    // car (extra)
    public static final String CAR_RESUME_NOT_ADMIN_PAUSED = "car.resume.notAdminPaused";

    // minimum rental days
    public static final String CAR_MIN_RENTAL_DAYS_EXCEEDS_PERIOD = "car.minRentalDays.exceedsPeriod";
    public static final String CAR_MIN_RENTAL_DAYS_INVALID = "car.minRentalDays.invalid";
    public static final String RESERVATION_RIDER_BELOW_MINIMUM_DAYS = "reservation.rider.belowMinimumDays";
    public static final String SEARCH_FLEXIBLE_MONTH_REQUIRED = "search.flexible.monthRequired";

    /** Catch-all for unmapped server failures (never expose raw {@code Throwable#getMessage()}). */
    public static final String ERROR_UNEXPECTED = "error.unexpected";

    /** Generic unauthorized (no raw Spring/Jersey message to clients). */
    public static final String ERROR_UNAUTHORIZED = "error.unauthorized";

    /** Generic forbidden (no raw Spring/Jersey message to clients). */
    public static final String ERROR_FORBIDDEN = "error.forbidden";

    /** Generic not found when the framework exception carries no i18n code. */
    public static final String ERROR_NOT_FOUND = "error.notFound";

    /** Invalid path/query/header parameter (Jersey {@code ParamException}). */
    public static final String ERROR_INVALID_REQUEST_PARAMETER = "error.invalidRequestParameter";
}
