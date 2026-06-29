package ar.edu.itba.paw.webapp.validation.reservation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.form.reservation.ReservationCreateForm;
import ar.edu.itba.paw.webapp.support.RestUriPaths;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.ReservationAvailabilityMatchesCar;

public final class ReservationAvailabilityMatchesCarValidator
        implements ConstraintValidator<ReservationAvailabilityMatchesCar, ReservationCreateForm> {

    @Override
    public boolean isValid(final ReservationCreateForm form, final ConstraintValidatorContext context) {
        if (form == null) {
            return true;
        }
        final String carUri = form.getCarUri();
        final String availabilityUri = form.getAvailabilityUri();
        if (carUri == null || carUri.isBlank() || availabilityUri == null || availabilityUri.isBlank()) {
            return true;
        }
        try {
            final long carId = RestUriPaths.parseCarId(carUri);
            final RestUriPaths.CarAvailabilityIds availabilityIds =
                    RestUriPaths.parseAvailabilityUri(availabilityUri);
            if (availabilityIds.carId() != carId) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                        .addPropertyNode("availabilityUri")
                        .addConstraintViolation();
                return false;
            }
            return true;
        } catch (final RuntimeException ex) {
            return true;
        }
    }
}
