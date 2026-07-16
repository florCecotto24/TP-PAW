package ar.edu.itba.paw.webapp.security.auth.exception;

import org.springframework.security.core.AuthenticationException;

/** Basic/OTP authentication rejected because the email is locked out after too many failed OTP guesses. */
public final class OtpRateLimitedAuthenticationException extends AuthenticationException {

    public OtpRateLimitedAuthenticationException(final Throwable cause) {
        super("otp_rate_limited", cause);
    }
}
