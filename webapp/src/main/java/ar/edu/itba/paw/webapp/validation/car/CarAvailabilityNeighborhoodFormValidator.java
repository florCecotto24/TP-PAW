package ar.edu.itba.paw.webapp.validation.car;

import ar.edu.itba.paw.services.location.LocationService;
import ar.edu.itba.paw.webapp.form.car.AvailabilityCreateForm;
import ar.edu.itba.paw.webapp.form.car.CarAvailabilityEditForm;
import ar.edu.itba.paw.webapp.form.car.CreateCarAvailabilityForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/** Spring {@link Validator} ensuring create/edit neighborhood id exists in {@link LocationService}. */
@Component
public final class CarAvailabilityNeighborhoodFormValidator implements Validator {

    private final LocationService locationService;

    public CarAvailabilityNeighborhoodFormValidator(final LocationService locationService) {
        this.locationService = locationService;
    }

    @Override
    public boolean supports(final Class<?> clazz) {
        return CreateCarAvailabilityForm.class.isAssignableFrom(clazz)
                || CarAvailabilityEditForm.class.isAssignableFrom(clazz)
                || AvailabilityCreateForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(final Object target, final Errors errors) {
        final Long neighborhoodId;
        if (target instanceof CreateCarAvailabilityForm createCarAvailabilityForm) {
            neighborhoodId = createCarAvailabilityForm.getNeighborhoodId();
        } else if (target instanceof CarAvailabilityEditForm carAvailabilityEditForm) {
            neighborhoodId = carAvailabilityEditForm.getNeighborhoodId();
        } else if (target instanceof AvailabilityCreateForm availabilityCreateForm) {
            neighborhoodId = availabilityCreateForm.getNeighborhoodId();
        } else {
            return;
        }
        if (neighborhoodId == null) {
            return;
        }
        if (locationService.findNeighborhoodById(neighborhoodId).isEmpty()) {
            errors.rejectValue("neighborhoodId", "validation.neighborhood.invalid");
        }
    }
}
