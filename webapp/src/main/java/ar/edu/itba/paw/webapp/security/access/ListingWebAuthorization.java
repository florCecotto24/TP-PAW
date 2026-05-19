package ar.edu.itba.paw.webapp.security.access;

import java.util.OptionalLong;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.ListingService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.security.http.HttpRequestPathIds;

/**
 * Authorization helpers for {@code /my-cars/**} (used with {@link AuthorizationManager} in security config).
 */
@Component("listingWebAuth")
public final class ListingWebAuthorization {

    private final ListingService listingService;

    public ListingWebAuthorization(final ListingService listingService) {
        this.listingService = listingService;
    }

    public boolean isOwner(final Authentication authentication, final HttpServletRequest request) {
        final Long userId = authenticatedUserId(authentication);
        if (userId == null) {
            return false;
        }
        final OptionalLong listingIdOpt = HttpRequestPathIds.firstLongSegmentAfterPrefix(request, "/my-cars/");
        if (!listingIdOpt.isPresent()) {
            return false;
        }
        final long listingId = listingIdOpt.getAsLong();
        return listingService.getListingDetailById(listingId)
                .map(d -> d.getOwner().getId() == userId)
                .orElse(false);
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
