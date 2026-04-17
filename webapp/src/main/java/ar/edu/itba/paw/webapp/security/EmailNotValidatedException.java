package ar.edu.itba.paw.webapp.security;

import org.springframework.security.core.AuthenticationException;

/**
 * Authentication exception for email not validated.
 * Extends {@link AuthenticationException} and must live together with the authentication provider, not in service contracts, to avoid coupling the domain to Spring Security.
 */
public final class EmailNotValidatedException extends AuthenticationException {

    private final String email;

    public EmailNotValidatedException(final String email) {
        super("Email not validated");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
