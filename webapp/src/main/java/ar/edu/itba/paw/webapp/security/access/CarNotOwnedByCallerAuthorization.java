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
 * Authorization helper for endpoints whose business rule requires the authenticated caller to
 * not be the owner of the {@code Car} identified by {@code carId}
 * (e.g. {@code POST /my-favorites/toggle} — you cannot favourite your own car;
 * {@code GET /cars/{carId}/reservation/new} — you cannot reserve your own car). Enforced at the
 * filter chain so the controller does not have to re-check the rule; the matching service-level
 * checks ({@code FavCarService#toggleFavorite}, {@code ReservationService#submitRiderReservationByCar})
 * remain as a domain invariant covering callers that arrive without going through the HTTP filter
 * (e.g. {@code POST /reservation} where {@code carId} travels in a multipart body).
 *
 * Reads {@code carId} from the request query/form parameter or from the {@code /cars/{carId}/…}
 * path prefix. Malformed / missing / unknown {@code carId} is intentionally allowed through so the
 * controller can surface a coherent validation error (e.g. {@code favCar.notFound},
 * {@code reservation.invalidCar}) rather than a generic 403.
 */
@Component("carNotOwnedByCallerAuth")
public final class CarNotOwnedByCallerAuthorization {

    private static final String CAR_ID_PARAM = "carId";
    private static final String PUBLIC_CAR_PATH_PREFIX = "/cars/";

    private final CarService carService;

    public CarNotOwnedByCallerAuthorization(final CarService carService) {
        this.carService = carService;
    }

    /**
     * {@code true} when the caller is authenticated and is NOT the owner of the car referenced by
     * {@code carId}. See class javadoc for the "missing/unknown is allowed" rationale.
     */
    public boolean isNotOwnerOfRequestCar(final Authentication authentication, final HttpServletRequest request) {
        final Long userId = authenticatedUserId(authentication);
        if (userId == null) {
            return false;
        }
        final Long carId = resolveCarId(request);
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

    private static Long resolveCarId(final HttpServletRequest request) {
        final Long fromParam = parseCarIdParam(request);
        if (fromParam != null) {
            return fromParam;
        }
        final OptionalLong fromPath = HttpRequestPathIds.firstLongSegmentAfterPrefix(request, PUBLIC_CAR_PATH_PREFIX);
        return fromPath.isPresent() ? fromPath.getAsLong() : null;
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
