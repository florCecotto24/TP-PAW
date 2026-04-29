package ar.edu.itba.paw.webapp.security.auth.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Indicates that a user without a password received a temporary password by email;
 * (extiende {@link AuthenticationException}).
 */
public final class LegacyPasswordMailedException extends AuthenticationException {

    public LegacyPasswordMailedException() {
        super("Legacy user password generated and emailed");
    }
}
