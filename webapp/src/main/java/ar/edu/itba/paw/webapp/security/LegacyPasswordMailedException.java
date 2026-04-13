package ar.edu.itba.paw.webapp.security;

import org.springframework.security.core.AuthenticationException;

/**
 * Indicates that the legacy user without password received a new password by email and must try to login again.
 */
public final class LegacyPasswordMailedException extends AuthenticationException {

    public LegacyPasswordMailedException() {
        super("Legacy user password generated and emailed");
    }
}
