package ar.edu.itba.paw.webapp.validation.car;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.support.CarRestEnums;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarType;

public final class ValidCarTypeValidator implements ConstraintValidator<ValidCarType, String> {

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            return CarRestEnums.parseType(value) != null;
        } catch (final IllegalArgumentException ex) {
            return false;
        }
    }
}
