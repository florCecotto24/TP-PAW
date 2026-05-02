package ar.edu.itba.paw.webapp.security.auth.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Thrown when a legacy account without a stored password was mailed a one-time password; extends
 * {@link AuthenticationException}.
 */
public final class LegacyPasswordMailedException extends AuthenticationException {

    public LegacyPasswordMailedException() {
        super("Legacy user password generated and emailed");
    }
}
