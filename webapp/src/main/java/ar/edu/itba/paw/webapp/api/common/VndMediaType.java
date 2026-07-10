package ar.edu.itba.paw.webapp.api.common;

/**
 * Vendor MIME types for REST content negotiation ({@code application/vnd.paw.*}).
 * Mirrors {@code openapi.yaml} and LINEAMIENTOS §1.7.
 */
public final class VndMediaType {

    public static final String API_V1_JSON = "application/vnd.paw.api.v1+json";
    public static final String USER_V1_JSON = "application/vnd.paw.user.v1+json";
    /** Link-only collection for {@code GET /users/{id}/favorites} (follow each {@code self} for car summary). */
    public static final String USER_FAVORITES_V1_JSON = "application/vnd.paw.user.favorites.v1+json";
    public static final String USER_PRIVATE_V1_JSON = "application/vnd.paw.user.private.v1+json";
    /** Body for admin-provisioned admin accounts (POST /users, admin-only Content-Type). */
    public static final String ADMIN_CREATE_USER_V1_JSON = "application/vnd.paw.admincreateuser.v1+json";
    public static final String CAR_SUMMARY_V1_JSON = "application/vnd.paw.car.summary.v1+json";
    /** Link-only collection for {@code GET /cars/{id}/similar} (follow each {@code self} for teasers). */
    public static final String CAR_SIMILAR_V1_JSON = "application/vnd.paw.car.similar.v1+json";
    public static final String CAR_V1_JSON = "application/vnd.paw.car.v1+json";
    public static final String AVAILABILITY_V1_JSON = "application/vnd.paw.availability.v1+json";
    public static final String BRAND_V1_JSON = "application/vnd.paw.brand.v1+json";
    public static final String MODEL_V1_JSON = "application/vnd.paw.model.v1+json";
    public static final String PRICE_MARKET_INSIGHT_V1_JSON = "application/vnd.paw.pricemarketinsight.v1+json";
    public static final String NEIGHBORHOOD_V1_JSON = "application/vnd.paw.neighborhood.v1+json";
    public static final String RESERVATION_SUMMARY_V1_JSON = "application/vnd.paw.reservation.summary.v1+json";
    /** Link-only collection for {@code GET /reservations} (follow each {@code self} with summary MIME). */
    public static final String RESERVATION_LINKS_V1_JSON = "application/vnd.paw.reservation.links.v1+json";
    public static final String RESERVATION_V1_JSON = "application/vnd.paw.reservation.v1+json";
    public static final String COUNTERPARTY_CONTACT_V1_JSON = "application/vnd.paw.counterpartycontact.v1+json";
    public static final String MESSAGE_V1_JSON = "application/vnd.paw.message.v1+json";
    public static final String REVIEW_V1_JSON = "application/vnd.paw.review.v1+json";
    /** Link-only collection for {@code GET /reviews} (follow each {@code self} for {@code review.v1}). */
    public static final String REVIEW_LINKS_V1_JSON = "application/vnd.paw.review.links.v1+json";
    public static final String PICTURE_V1_JSON = "application/vnd.paw.picture.v1+json";
    public static final String BOOKABLE_SEGMENT_V1_JSON = "application/vnd.paw.bookablesegment.v1+json";
    public static final String CREDENTIAL_V1_JSON = "application/vnd.paw.credential.v1+json";
    public static final String ERROR_V1_JSON = "application/vnd.paw.error.v1+json";
    public static final String VALIDATION_ERROR_V1_JSON = "application/vnd.paw.validation-error.v1+json";

    private VndMediaType() {
    }
}
