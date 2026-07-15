package ar.edu.itba.paw.webapp.support;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * Resolves the authenticated principal from the stateless JWT security context inside JAX-RS resources.
 */
@Component
public final class CurrentUserResolver {

    public RydenUserDetails requirePrincipal() {
        final RydenUserDetails details = currentPrincipalOrNull();
        if (details == null) {
            // 2-arg overload: (message, challenge). The 1-arg form would treat its String as the
            // WWW-Authenticate challenge, emitting a malformed header; pass "Bearer" explicitly (RFC 7235).
            throw new javax.ws.rs.NotAuthorizedException("Authentication required.", "Bearer");
        }
        return details;
    }

    public long requireUserId() {
        return requirePrincipal().getUserId();
    }

    public RydenUserDetails currentPrincipalOrNull() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        if (authentication.getPrincipal() instanceof RydenUserDetails details) {
            return details;
        }
        return null;
    }

    public Long currentUserIdOrNull() {
        final RydenUserDetails details = currentPrincipalOrNull();
        return details == null ? null : details.getUserId();
    }
}
