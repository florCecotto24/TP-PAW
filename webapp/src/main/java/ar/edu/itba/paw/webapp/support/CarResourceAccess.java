package ar.edu.itba.paw.webapp.support;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.webapp.security.auth.AuthenticationAuthorities;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * Authorization predicates for car resources.
 *
 * See {@link UserResourceAccess} for the {@code is*} vs. {@code require*} split rationale. The
 * {@code *ById}
 * variants exist for {@code @PreAuthorize}, which runs before the resource method body loads the
 * {@link Car} entity: they resolve the car themselves and, if it does not exist, allow the request
 * through so the resource's own lookup raises the 404 — this keeps "not found" taking precedence
 * over "forbidden", identical to the pre-existing imperative checks (which always loaded the car
 * first and 404'd before ever reaching the ownership check).
 */
@Component
public final class CarResourceAccess {

    private static final String ACCESS_DENIED_MESSAGE = "You do not have permission to perform this action.";

    private final CarService carService;

    public CarResourceAccess(final CarService carService) {
        this.carService = carService;
    }

    public boolean isAdmin() {
        return AuthenticationAuthorities.hasAdminRole(SecurityContextHolder.getContext().getAuthentication());
    }

    public boolean isOwnerOrAdmin(final Car car, final RydenUserDetails viewer) {
        if (viewer == null) {
            return false;
        }
        if (car.getOwnerId() == viewer.getUserId()) {
            return true;
        }
        return isAdmin();
    }

    public boolean isOwner(final Car car, final RydenUserDetails viewer) {
        return viewer != null && car.getOwnerId() == viewer.getUserId();
    }

    public boolean isOwnerById(final long carId, final RydenUserDetails viewer) {
        return carService.getCarById(carId).map(car -> isOwner(car, viewer)).orElse(true);
    }

    public boolean isOwnerOrAdminById(final long carId, final RydenUserDetails viewer) {
        return carService.getCarById(carId).map(car -> isOwnerOrAdmin(car, viewer)).orElse(true);
    }

    public void requireOwnerOrAdmin(final Car car, final RydenUserDetails viewer) {
        if (!isOwnerOrAdmin(car, viewer)) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
    }

    public void requireOwner(final Car car, final RydenUserDetails viewer) {
        if (!isOwner(car, viewer)) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
    }
}
