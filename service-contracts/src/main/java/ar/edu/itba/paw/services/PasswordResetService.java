package ar.edu.itba.paw.services;

import java.util.Locale;

public interface PasswordResetService {

    /**
     * Sends a 6-digit code. If there is an active code, throws an exception.
     *
     * @return {@code true} if the email belongs to a user; {@code false} if there is no account (do not reveal to the client).
     */
    boolean requestCode(String email, Locale locale);

    /**
     * Validates the code, updates the password (BCrypt) and deletes the reset codes for the user.
     */
    void resetPassword(String email, String code, String newPassword, String newPasswordConfirm);
}
