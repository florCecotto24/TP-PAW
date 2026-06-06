package ar.edu.itba.paw.services.car.view;


import java.util.Locale;
import java.util.Optional;

import ar.edu.itba.paw.models.dto.car.OwnerCarDetailPageModel;

import ar.edu.itba.paw.services.car.CarService;
/**
 * Materialises the owner-facing /my-cars/{carId} dashboard page model out of the various
 * services (cars, availability, reservations, location, pictures). Extracted from
 * {@link CarService} so the latter stays focused on car lifecycle/mutations.
 *
 * <p>{@link CarService#buildOwnerCarDetailPageModel(long, Locale)} delegates here (back-compat).</p>
 */
public interface OwnerCarDetailViewService {

    /** See {@link CarService#buildOwnerCarDetailPageModel(long, Locale)}. */
    Optional<OwnerCarDetailPageModel> buildOwnerCarDetailPageModel(long carId, Locale locale);
}
