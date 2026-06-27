package ar.edu.itba.paw.webapp.security.auth;

/**
 * Extra Spring Security authorities used during OTP-based flows (password reset).
 */
public final class RydenAuthorities {

    /** Authenticated with a valid password-reset OTP (not yet consumed). */
    public static final String PASSWORD_RESET_OTP = "ROLE_PASSWORD_RESET_OTP";

    private RydenAuthorities() {
    }
}
