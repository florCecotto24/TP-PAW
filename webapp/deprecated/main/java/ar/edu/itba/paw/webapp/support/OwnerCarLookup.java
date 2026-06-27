package ar.edu.itba.paw.webapp.support;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.services.car.CarService;

/**
 * Loads a {@link Car} owned by the current viewer for the owner-only handlers in
 * {@code MyCarsController}.
 *
 * <p>Access control (ownership) is enforced by the Spring Security filter chain via
 * {@link ar.edu.itba.paw.webapp.security.access.CarOwnerWebAuthorization} on the
 * {@code /my-cars/car/{carId}/**} and {@code /my-cars/reservations/{carId}} matchers
 * (see {@code WebAuthConfig#securityFilterChain}). By the time a controller calls
 * {@link #loadOwnedCar(long, String)}, the caller is guaranteed to own {@code carId}.
 *
 * <p>This helper exists only to keep loading + the rare not-found redirect out of the
 * controller body: it does <em>not</em> re-validate ownership. If the row disappeared
 * between the filter-chain check and the controller call (concurrent deactivation, etc.)
 * the helper returns a ready-to-use redirect to a safe fallback URL instead of
 * propagating a {@link RuntimeException}.
 */
@Component
public final class OwnerCarLookup {

    private final CarService carService;

    public OwnerCarLookup(final CarService carService) {
        this.carService = carService;
    }

    /**
     * Loads the {@link Car} identified by {@code carId}. Ownership is assumed (enforced by the
     * Spring Security filter chain). Returns either the resolved car or a redirect to
     * {@code redirectIfMissing} for the rare case where the car no longer exists.
     *
     * @param carId             path-variable car id
     * @param redirectIfMissing absolute in-app URL (e.g. {@code "/my-cars"}) to redirect to when
     *                          the car has been removed between the filter check and the
     *                          controller call
     */
    public Result loadOwnedCar(final long carId, final String redirectIfMissing) {
        final Optional<Car> carOpt = carService.getCarById(carId);
        if (carOpt.isEmpty()) {
            return Result.redirect(redirectIfMissing);
        }
        return Result.ok(carOpt.get());
    }

    /** Either the resolved {@link Car} or a redirect to take the viewer elsewhere. */
    public static final class Result {

        private final Car carOrNull;
        private final ModelAndView redirectOrNull;

        private Result(final Car carOrNull, final ModelAndView redirectOrNull) {
            this.carOrNull = carOrNull;
            this.redirectOrNull = redirectOrNull;
        }

        public Optional<Car> car() { return Optional.ofNullable(carOrNull); }

        public Optional<ModelAndView> redirect() { return Optional.ofNullable(redirectOrNull); }

        static Result ok(final Car car) { return new Result(car, null); }

        static Result redirect(final String url) {
            final RedirectView rv = new RedirectView(url, true);
            rv.setExposeModelAttributes(false);
            return new Result(null, new ModelAndView(rv));
        }
    }
}
