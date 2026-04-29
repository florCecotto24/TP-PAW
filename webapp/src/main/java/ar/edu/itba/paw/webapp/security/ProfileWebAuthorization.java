
package ar.edu.itba.paw.webapp.security;

import ar.edu.itba.paw.services.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.OptionalLong;
import java.util.function.Supplier;

@Component("profileWebAuth")
public class ProfileWebAuthorization {

    private final UserService userService;

    public ProfileWebAuthorization(final UserService userService) {
        this.userService = userService;
    }

    public boolean isOwner(final Authentication authentication, final HttpServletRequest request) {
        final Long userId = authenticatedUserId(authentication);
        if (userId == null) {
            return false;
        }
        final OptionalLong requestUserId = HttpRequestPathIds.firstLongSegmentAfterPrefix(request, "/profile/");
        if (!requestUserId.isPresent()) {
            return false;
        }
        return userId.equals(requestUserId.getAsLong());
    }

    public AuthorizationManager<RequestAuthorizationContext> ownerAccess() {
        return (Supplier<Authentication> authentication, RequestAuthorizationContext context) -> {
            final Authentication auth = authentication.get();
            if (auth == null || !auth.isAuthenticated()) {
                return new AuthorizationDecision(false);
            }
            return new AuthorizationDecision(isOwner(auth, context.getRequest()));
        };
    }

    private static Long authenticatedUserId(final Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        if (!(authentication.getPrincipal() instanceof RydenUserDetails)) {
            return null;
        }
        return ((RydenUserDetails) authentication.getPrincipal()).getUserId();
    }
}
