package ar.edu.itba.paw.webapp.security.auth.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Basic/OTP authentication rejected because the email-verification one-time code is wrong or expired.
 */
public final class InvalidVerificationCodeAuthenticationException extends AuthenticationException {

    public InvalidVerificationCodeAuthenticationException(final Throwable cause) {
        super("user.verification.codeInvalid", cause);
    }
}
