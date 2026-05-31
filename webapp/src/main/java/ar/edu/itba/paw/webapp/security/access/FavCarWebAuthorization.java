package ar.edu.itba.paw.webapp.security.access;

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

/**
 * Authorization helper for {@code POST /my-favorites/toggle}: the authenticated user must NOT be
 * the owner of the {@code Car} referenced by the {@code carId} request parameter. Mirrors the
 * business rule enforced in {@link ar.edu.itba.paw.services.FavCarService#toggleFavorite}, adding
 * defense-in-depth at the filter chain. Wired into the security config via
 * {@link AuthorizationManager}.
 */
@Component("favCarWebAuth")
public final class FavCarWebAuthorization {

    private static final String CAR_ID_PARAM = "carId";

    private final CarService carService;

    public FavCarWebAuthorization(final CarService carService) {
        this.carService = carService;
    }

    /**
     * {@code true} when the caller is authenticated and is NOT the owner of the car referenced by
     * the {@code carId} request parameter. Malformed/missing {@code carId} or non-existent cars are
     * allowed through so the controller can surface a coherent validation error
     * (e.g. {@code favCar.notFound}) instead of a generic 403.
     */
    public boolean isNotOwnerOfRequestCar(final Authentication authentication, final HttpServletRequest request) {
        final Long userId = authenticatedUserId(authentication);
        if (userId == null) {
            return false;
        }
        final Long carId = parseCarIdParam(request);
        if (carId == null) {
            return true;
        }
        return carService.getCarById(carId)
                .map(c -> c.getOwnerId() != userId)
                .orElse(true);
    }

    public AuthorizationManager<RequestAuthorizationContext> nonOwnerAccess() {
        return (Supplier<Authentication> authentication, RequestAuthorizationContext context) -> {
            final Authentication auth = authentication.get();
            if (auth == null || !auth.isAuthenticated()) {
                return new AuthorizationDecision(false);
            }
            return new AuthorizationDecision(isNotOwnerOfRequestCar(auth, context.getRequest()));
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

    private static Long parseCarIdParam(final HttpServletRequest request) {
        final String raw = request.getParameter(CAR_ID_PARAM);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            final long id = Long.parseLong(raw.trim());
            return id > 0 ? id : null;
        } catch (final NumberFormatException e) {
            return null;
        }
    }
}
