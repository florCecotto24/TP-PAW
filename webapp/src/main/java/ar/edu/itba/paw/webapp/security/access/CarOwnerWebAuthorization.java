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
 * Authorization helper for {@code /my-cars/car/{carId}/**}: grants access only when the authenticated
 * user owns the {@code Car} referenced by the path. Wired into the security config via
 * {@link AuthorizationManager}.
 */
@Component("carOwnerWebAuth")
public final class CarOwnerWebAuthorization {

    private final CarService carService;

    public CarOwnerWebAuthorization(final CarService carService) {
        this.carService = carService;
    }

    public boolean isOwner(final Authentication authentication, final HttpServletRequest request) {
        final Long userId = authenticatedUserId(authentication);
        if (userId == null) {
            return false;
        }
        final OptionalLong carIdOpt = HttpRequestPathIds.firstLongSegmentAfterPrefix(request, "/my-cars/car/");
        if (carIdOpt.isEmpty()) {
            return false;
        }
        return carService.getCarById(carIdOpt.getAsLong())
                .map(c -> c.getOwnerId() == userId)
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
