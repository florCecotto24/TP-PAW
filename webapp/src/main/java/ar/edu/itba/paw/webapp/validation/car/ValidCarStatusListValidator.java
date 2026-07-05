package ar.edu.itba.paw.webapp.validation.car;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarStatusList;

public final class ValidCarStatusListValidator implements ConstraintValidator<ValidCarStatusList, List<String>> {

    private final ValidCarStatusValidator delegate = new ValidCarStatusValidator();

    @Override
    public boolean isValid(final List<String> values, final ConstraintValidatorContext context) {
        if (values == null || values.isEmpty()) {
            return true;
        }
        for (final String value : values) {
            if (!delegate.isValid(value, context)) {
                return false;
            }
        }
        return true;
    }
}
