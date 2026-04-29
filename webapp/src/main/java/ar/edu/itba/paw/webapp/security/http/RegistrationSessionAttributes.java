package ar.edu.itba.paw.webapp.security.http;

public final class RegistrationSessionAttributes {

    public static final String PENDING_PASSWORD_USER_ID = "PENDING_PASSWORD_USER_ID";

    /** After a login with an email not validated; it is consumed in GET /verify-email. */
    public static final String PENDING_VERIFY_EMAIL = "PENDING_VERIFY_EMAIL";

    private RegistrationSessionAttributes() {
    }
}
