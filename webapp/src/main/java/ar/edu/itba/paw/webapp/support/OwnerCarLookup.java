package ar.edu.itba.paw.webapp.support;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.services.CarService;

/**
 * Tiny lookup helper used by the owner-only car handlers in {@code MyCarsController}: every
 * such handler used to inline the same {@code carService.getCarById + ownership check + redirect
 * to /my-cars (or /my-cars/car/{id})} block (repeated in 10+ places). The helper returns a
 * resolved {@link Car} or a ready-to-return {@link ModelAndView} redirect so controllers can
 * branch with a single {@code if (result.redirect().isPresent()) return result.redirect().get();}.
 */
@Component
public final class OwnerCarLookup {

    private final CarService carService;

    public OwnerCarLookup(final CarService carService) {
        this.carService = carService;
    }

    /**
     * Resolves {@code carId} as a car owned by {@code viewerUserId}.
     *
     * @param viewerUserId       authenticated user id
     * @param carId              path-variable car id
     * @param redirectIfMissing  absolute URL (e.g. {@code "/my-cars"}) to redirect to when the
     *                           car does not exist or is not owned by the viewer
     */
    public Result resolveOwnedCar(
            final long viewerUserId, final long carId, final String redirectIfMissing) {
        final Optional<Car> carOpt = carService.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != viewerUserId) {
            return Result.redirect(redirectIfMissing);
        }
        return Result.ok(carOpt.get());
    }

    /** Either the resolved {@link Car} (owner matches) or a redirect to take the viewer elsewhere. */
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
