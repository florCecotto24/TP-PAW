package ar.edu.itba.paw.services;

import java.util.Locale;

public interface PasswordResetService {

    /**
     * Starts password recovery for the given address. The service enforces its own policy; delivery details stay inside the service layer.
     * @return {@code true} if a matching account exists, {@code false} otherwise (caller may not distinguish for security)
     */
    boolean initiatePasswordReset(String email, Locale locale);

    /** Completes password recovery after the user provides the code and a new password pair. */
    void completePasswordReset(String email, String code, String newPassword, String newPasswordConfirm);
}
