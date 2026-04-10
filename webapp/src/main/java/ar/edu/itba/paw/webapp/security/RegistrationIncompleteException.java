package ar.edu.itba.paw.webapp.security;

import org.springframework.security.core.AuthenticationException;

public final class RegistrationIncompleteException extends AuthenticationException {

    private final long userId;

    public RegistrationIncompleteException(final long userId) {
        super("Registration incomplete: password required");
        this.userId = userId;
    }

    public long getUserId() {
        return userId;
    }
}
