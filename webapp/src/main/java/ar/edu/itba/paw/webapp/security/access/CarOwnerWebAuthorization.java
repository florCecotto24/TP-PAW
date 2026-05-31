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
 * Authorization helper for URL families that carry a numeric {@code carId} as the first segment
 * after a known prefix (e.g. {@code /my-cars/car/{carId}/**} and {@code /my-cars/reservations/{carId}}):
 * grants access only when the authenticated user owns that {@code Car}. Wired into the security
 * config via {@link AuthorizationManager}.
 *
 * <p>The prefix is parametrised at {@link AuthorizationManager} creation time so different URL
 * families that all encode {@code carId} the same way can reuse the same component (filter-chain
 * defense-in-depth on top of the controller-level {@code OwnerCarLookup} branch).</p>
 */
@Component("carOwnerWebAuth")
public final class CarOwnerWebAuthorization {

    /** Default prefix kept as the canonical car-detail family. */
    public static final String MY_CARS_DETAIL_PREFIX = "/my-cars/car/";

    private final CarService carService;

    public CarOwnerWebAuthorization(final CarService carService) {
        this.carService = carService;
    }

    public boolean isOwnerForPrefix(
            final Authentication authentication,
            final HttpServletRequest request,
            final String prefix) {
        final Long userId = authenticatedUserId(authentication);
        if (userId == null) {
            return false;
        }
        final OptionalLong carIdOpt = HttpRequestPathIds.firstLongSegmentAfterPrefix(request, prefix);
        if (carIdOpt.isEmpty()) {
            return false;
        }
        return carService.getCarById(carIdOpt.getAsLong())
                .map(c -> c.getOwnerId() == userId)
                .orElse(false);
    }

    /**
     * Convenience overload for the default {@link #MY_CARS_DETAIL_PREFIX} family
     * ({@code /my-cars/car/{carId}/**}).
     */
    public AuthorizationManager<RequestAuthorizationContext> ownerAccess() {
        return ownerAccessForPrefix(MY_CARS_DETAIL_PREFIX);
    }

    /**
     * Builds an {@link AuthorizationManager} that grants access when the caller owns the {@code Car}
     * whose id is the first path segment after {@code prefix}. {@code prefix} must start and end
     * with {@code /} (see {@link HttpRequestPathIds#firstLongSegmentAfterPrefix}).
     */
    public AuthorizationManager<RequestAuthorizationContext> ownerAccessForPrefix(final String prefix) {
        return (Supplier<Authentication> authentication, RequestAuthorizationContext context) -> {
            final Authentication auth = authentication.get();
            if (auth == null || !auth.isAuthenticated()) {
                return new AuthorizationDecision(false);
            }
            return new AuthorizationDecision(isOwnerForPrefix(auth, context.getRequest(), prefix));
        };
    }

    private static Long authenticatedUserId(final Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        if (!(authentication.getPrincipal() instanceof RydenUserDetails details)) {
            return null;
        }
        return details.getUserId();
    }
}
