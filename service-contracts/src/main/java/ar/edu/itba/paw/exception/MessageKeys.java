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
    public static final String RESERVATION_RIDER_OUTSIDE_AVAILABILITY = "reservation.rider.outsideAvailability";
    public static final String RESERVATION_FORM_CAR_NAME_REQUIRED = "reservation.form.carNameRequired";

    /* user */
    public static final String USER_EMAIL_ALREADY_EXISTS = "user.email.alreadyExists";

    /* listing */
    public static final String LISTING_AVAILABILITY_REQUIRED = "listing.availability.required";
    public static final String LISTING_AVAILABILITY_INVALID_ORDER = "listing.availability.invalidOrder";
    public static final String LISTING_CAR_NOT_FOUND = "listing.carNotFound";
    public static final String LISTING_LIMIT_POSITIVE = "listing.limit.positive";

    /* image */
    public static final String IMAGE_INVALID_ID = "image.invalidId";

    /* publish form */
    public static final String PUBLISH_IMAGES_READ = "publish.images.read";
    public static final String PUBLISH_FAILED = "publish.failed";
}
