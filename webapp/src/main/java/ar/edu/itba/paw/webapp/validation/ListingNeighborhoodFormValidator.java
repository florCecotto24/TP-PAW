package ar.edu.itba.paw.webapp.validation;

import ar.edu.itba.paw.services.LocationService;
import ar.edu.itba.paw.webapp.form.CreateListingForm;
import ar.edu.itba.paw.webapp.form.ListingEditForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/** Spring {@link Validator} ensuring create/edit neighborhood id exists in {@link LocationService}. */
@Component
public final class ListingNeighborhoodFormValidator implements Validator {

    private final LocationService locationService;

    public ListingNeighborhoodFormValidator(final LocationService locationService) {
        this.locationService = locationService;
    }

    @Override
    public boolean supports(final Class<?> clazz) {
        return CreateListingForm.class.isAssignableFrom(clazz) || ListingEditForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(final Object target, final Errors errors) {
        final Long neighborhoodId;
        if (target instanceof CreateListingForm) {
            neighborhoodId = ((CreateListingForm) target).getNeighborhoodId();
        } else if (target instanceof ListingEditForm) {
            neighborhoodId = ((ListingEditForm) target).getNeighborhoodId();
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
