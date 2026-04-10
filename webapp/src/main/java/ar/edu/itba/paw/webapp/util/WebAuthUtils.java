package ar.edu.itba.paw.webapp.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

import ar.edu.itba.paw.webapp.security.RydenUserDetails;

public final class WebAuthUtils {

    private WebAuthUtils() {
    }

    public static RydenUserDetails requireCurrentUser(final Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof RydenUserDetails)) {
            throw new IllegalStateException("Expected authenticated user");
        }
        return (RydenUserDetails) authentication.getPrincipal();
    }
}
