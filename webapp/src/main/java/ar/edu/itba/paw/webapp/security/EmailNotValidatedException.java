package ar.edu.itba.paw.webapp.security;

import org.springframework.security.core.AuthenticationException;

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
