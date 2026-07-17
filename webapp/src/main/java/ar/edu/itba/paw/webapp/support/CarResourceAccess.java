package ar.edu.itba.paw.webapp.support;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.webapp.security.auth.AuthenticationAuthorities;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * Authorization predicates for car resources.
 *
 * See {@link UserResourceAccess} for the {@code is*} vs. {@code require*} split rationale. The
 * {@code *ById}
 * variants exist for {@code @PreAuthorize}, which runs before the resource method body loads the
 * {@link Car} entity: they resolve the car themselves and, if it does not exist, allow the request
 * through so the resource's own lookup raises the 404. When the car exists but the viewer is not
 * authorized, they raise {@link CarNotFoundException} so "not found" masks "forbidden" (same policy
 * as {@link #requireViewableCar}).
 *
 * Public browse stays limited to {@link Car.Status#ACTIVE}. Reservation riders may also read a car
 * so hypermedia {@code links.car} on reservation DTOs remains followable after the listing is paused
 * or deactivated.
 */
@Component
public final class CarResourceAccess {

    private static final String ACCESS_DENIED_MESSAGE = "You do not have permission to perform this action.";

    private final CarService carService;
    private final ReservationService reservationService;

    public CarResourceAccess(final CarService carService, final ReservationService reservationService) {
        this.carService = carService;
        this.reservationService = reservationService;
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

    /** Only {@link Car.Status#ACTIVE} cars are readable without owner/admin/rider context (public browse). */
    public boolean isPubliclyReadable(final Car car) {
        return car.getStatus() == Car.Status.ACTIVE;
    }

    /**
     * Read access to a car item and its public sub-resources ({@code GET /cars/{id}},
     * gallery metadata, availabilities). Non-active cars are visible to the owner, admin, or a
     * signed-in rider who has any reservation for that car.
     */
    public boolean canViewCar(final Car car, final RydenUserDetails viewer) {
        if (isPubliclyReadable(car) || isOwnerOrAdmin(car, viewer)) {
            return true;
        }
        return viewer != null
                && reservationService.existsRiderReservationForCar(viewer.getUserId(), car.getId());
    }

    public boolean canViewCarById(final long carId, final RydenUserDetails viewer) {
        return carService.getCarById(carId).map(car -> canViewCar(car, viewer)).orElse(true);
    }

    /** Read predicate for already-resolved related-resource ids; missing cars are not viewable. */
    public boolean canViewExistingCarById(final long carId, final RydenUserDetails viewer) {
        return carService.getCarById(carId).map(car -> canViewCar(car, viewer)).orElse(false);
    }

    /**
     * Loads a car for public read sub-resources ({@code GET /cars/{id}/…}). Missing cars and cars the
     * viewer cannot read both raise {@link CarNotFoundException} so "not found" masks "forbidden".
     */
    public Car requireViewableCar(final long carId, final RydenUserDetails viewer) {
        final Car car = carService.getCarById(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
        if (!canViewCar(car, viewer)) {
            throw new CarNotFoundException(carId);
        }
        return car;
    }

    public void requireCanViewCar(final Car car, final RydenUserDetails viewer) {
        if (!canViewCar(car, viewer)) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
    }

    public boolean isOwner(final Car car, final RydenUserDetails viewer) {
        return viewer != null && car.getOwnerId() == viewer.getUserId();
    }

    /**
     * Owner gate for mutations. Missing cars return {@code true} so the resource body can 404.
     * Existing cars the viewer does not own raise {@link CarNotFoundException} (same 404 mask as
     * {@link #requireViewableCar}) instead of a 403 existence oracle.
     */
    public boolean isOwnerById(final long carId, final RydenUserDetails viewer) {
        final Car car = carService.getCarById(carId).orElse(null);
        if (car == null) {
            return true;
        }
        if (!isOwner(car, viewer)) {
            throw new CarNotFoundException(carId);
        }
        return true;
    }

    /**
     * Owner-or-admin gate. Same 404-masking policy as {@link #isOwnerById} when the car exists
     * but the viewer is neither owner nor admin.
     */
    public boolean isOwnerOrAdminById(final long carId, final RydenUserDetails viewer) {
        final Car car = carService.getCarById(carId).orElse(null);
        if (car == null) {
            return true;
        }
        if (!isOwnerOrAdmin(car, viewer)) {
            throw new CarNotFoundException(carId);
        }
        return true;
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
