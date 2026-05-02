package ar.edu.itba.paw.services;

import java.util.Locale;

/**
 * Email verification codes during registration and first-login flows.
 * Implementations use {@code EmailVerificationCodeDao} only; user rows and mail go through {@code UserService} and {@code EmailService}.
 */
public interface EmailVerificationService {

    /** Invalidates previous codes and sends a new one (step 2 of registration). */
    void issueFreshVerificationCode(long userId, String email, Locale locale);

    /**
     * If there is no active code, generates and sends one (e.g. login with unverified email).
     */
    void ensurePendingVerificationCode(long userId, String email, Locale locale);

    /**
     * Explicit re-send: fails if there is an active code.
     */
    void resendVerificationCode(long userId, String email, Locale locale);

    /**
     * Validates the code for {@code email}, marks the account verified, and returns the user id.
     *
     * @throws ar.edu.itba.paw.exception.RydenException when the code is unknown, expired, or already consumed
     */
    long verifyEmailAndConsumeCode(String email, String code);
}
