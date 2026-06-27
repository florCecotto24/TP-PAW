package ar.edu.itba.paw.webapp.validation.car;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.form.car.CarPatchForm;
import ar.edu.itba.paw.webapp.validation.constraint.car.CarPatchHasField;

public final class CarPatchHasFieldValidator implements ConstraintValidator<CarPatchHasField, CarPatchForm> {

    @Override
    public boolean isValid(final CarPatchForm form, final ConstraintValidatorContext context) {
        if (form == null) {
            return false;
        }
        return (form.getStatus() != null && !form.getStatus().isBlank())
                || form.getDescription() != null
                || form.getMinimumRentalDays() != null;
    }
}
