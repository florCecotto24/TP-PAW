package ar.edu.itba.paw.webapp.security.access;

import java.util.function.Supplier;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * Authorization for {@code /profile/**}. {@link ar.edu.itba.paw.webapp.controller.ProfileController} always
 * acts on the authenticated account (no user id in the path): only that principal can edit "their"
 * profile via these URLs. This manager rejects anonymous callers and principals that are not
 * {@link RydenUserDetails}, matching {@link ReservationWebAuthorization} conventions.
 */
@Component("profileWebAuth")
public final class ProfileWebAuthorization {

    /**
     * Allows access only when the caller is authenticated with a {@link RydenUserDetails} principal —
     * the profile endpoints always load/update the backing user tied to that principal.
     */
    public AuthorizationManager<RequestAuthorizationContext> selfProfileAccess() {
        return (Supplier<Authentication> authentication, RequestAuthorizationContext context) -> {
            final Authentication auth = authentication.get();
            if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                return new AuthorizationDecision(false);
            }
            if (!(auth.getPrincipal() instanceof RydenUserDetails)) {
                return new AuthorizationDecision(false);
            }
            return new AuthorizationDecision(true);
        };
    }
}
