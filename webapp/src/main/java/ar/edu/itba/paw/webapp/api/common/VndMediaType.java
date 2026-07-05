package ar.edu.itba.paw.webapp.api.common;

/**
 * Vendor MIME types for REST content negotiation ({@code application/vnd.paw.*}).
 * Mirrors {@code openapi.yaml} and LINEAMIENTOS §1.7.
 */
public final class VndMediaType {

    public static final String API_V1_JSON = "application/vnd.paw.api.v1+json";
    public static final String USER_V1_JSON = "application/vnd.paw.user.v1+json";
    public static final String USER_V1_XML = "application/vnd.paw.user.v1+xml";
    public static final String USER_PRIVATE_V1_JSON = "application/vnd.paw.user.private.v1+json";
    /** Body for admin-provisioned admin accounts (POST /users, admin-only Content-Type). */
    public static final String ADMIN_CREATE_USER_V1_JSON = "application/vnd.paw.admincreateuser.v1+json";
    public static final String CAR_V1_JSON = "application/vnd.paw.car.v1+json";
    public static final String CAR_V1_XML = "application/vnd.paw.car.v1+xml";
    public static final String AVAILABILITY_V1_JSON = "application/vnd.paw.availability.v1+json";
    public static final String BRAND_V1_JSON = "application/vnd.paw.brand.v1+json";
    public static final String MODEL_V1_JSON = "application/vnd.paw.model.v1+json";
    public static final String PRICE_MARKET_INSIGHT_V1_JSON = "application/vnd.paw.pricemarketinsight.v1+json";
    public static final String NEIGHBORHOOD_V1_JSON = "application/vnd.paw.neighborhood.v1+json";
    public static final String RESERVATION_V1_JSON = "application/vnd.paw.reservation.v1+json";
    public static final String COUNTERPARTY_CONTACT_V1_JSON = "application/vnd.paw.counterpartycontact.v1+json";
    public static final String MESSAGE_V1_JSON = "application/vnd.paw.message.v1+json";
    public static final String REVIEW_V1_JSON = "application/vnd.paw.review.v1+json";
    public static final String PICTURE_V1_JSON = "application/vnd.paw.picture.v1+json";
    public static final String BOOKABLE_SEGMENT_V1_JSON = "application/vnd.paw.bookablesegment.v1+json";
    public static final String PASSWORD_RESET_CODE_V1_JSON = "application/vnd.paw.passwordresetcode.v1+json";
    public static final String CREDENTIAL_V1_JSON = "application/vnd.paw.credential.v1+json";
    public static final String ERROR_V1_JSON = "application/vnd.paw.error.v1+json";

    private VndMediaType() {
    }
}
