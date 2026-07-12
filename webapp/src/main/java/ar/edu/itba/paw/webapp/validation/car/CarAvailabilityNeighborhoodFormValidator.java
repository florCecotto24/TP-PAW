package ar.edu.itba.paw.webapp.validation.car;

import ar.edu.itba.paw.services.location.LocationService;
import ar.edu.itba.paw.webapp.form.car.AvailabilityCreateForm;
import ar.edu.itba.paw.webapp.util.RestUriUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Optional;

/** Spring {@link Validator} ensuring create/edit neighborhood id exists in {@link LocationService}. */
@Component
public final class CarAvailabilityNeighborhoodFormValidator implements Validator {

    private final LocationService locationService;

    public CarAvailabilityNeighborhoodFormValidator(final LocationService locationService) {
        this.locationService = locationService;
    }

    @Override
    public boolean supports(final Class<?> clazz) {
        return AvailabilityCreateForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(final Object target, final Errors errors) {
        if (!(target instanceof AvailabilityCreateForm availabilityCreateForm)) {
            return;
        }
        final Optional<Long> neighborhoodId =
                RestUriUtils.parseNeighborhoodId(availabilityCreateForm.getNeighborhoodUri());
        if (neighborhoodId.isEmpty()) {
            errors.rejectValue("neighborhoodUri", "validation.neighborhood.invalid");
            return;
        }
        if (locationService.findNeighborhoodById(neighborhoodId.get()).isEmpty()) {
            errors.rejectValue("neighborhoodUri", "validation.neighborhood.invalid");
        }
    }
}
