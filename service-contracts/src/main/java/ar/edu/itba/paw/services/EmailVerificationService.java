package ar.edu.itba.paw.services;

import java.util.Locale;

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

    long verifyEmailAndConsumeCode(String email, String code);
}
