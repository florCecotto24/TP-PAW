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

import ar.edu.itba.paw.services.CarService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.security.http.HttpRequestPathIds;

/**
 * Authorization helpers for {@code /my-cars/**} (used with {@link AuthorizationManager} in security config).
 */
@Component("listingWebAuth")
public final class ListingWebAuthorization {

    private final CarService carService;

    public ListingWebAuthorization(final CarService carService) {
        this.carService = carService;
    }

    public boolean isOwner(final Authentication authentication, final HttpServletRequest request) {
        final Long userId = authenticatedUserId(authentication);
        if (userId == null) {
            return false;
        }
        // New car-centric path: /my-cars/car/{carId}/...
        final String path = request.getServletPath() != null ? request.getServletPath() : request.getRequestURI();
        final OptionalLong carIdOpt = HttpRequestPathIds.firstLongSegmentAfterPrefix(request, "/my-cars/car/");
        if (carIdOpt.isPresent()) {
            return carService.getCarById(carIdOpt.getAsLong())
                    .map(c -> c.getOwnerId() == userId)
                    .orElse(false);
        }
        // Legacy path: /my-cars/{listingId}/... (still supported during Phase 7d transition)
        final OptionalLong legacyIdOpt = HttpRequestPathIds.firstLongSegmentAfterPrefix(request, "/my-cars/");
        if (legacyIdOpt.isPresent()) {
            return carService.getCarById(legacyIdOpt.getAsLong())
                    .map(c -> c.getOwnerId() == userId)
                    .orElse(false);
        }
        return false;
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
