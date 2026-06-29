package ar.edu.itba.paw.webapp.validation.car;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarTypeList;

public final class ValidCarTypeListValidator implements ConstraintValidator<ValidCarTypeList, List<String>> {

    private final ValidCarTypeValidator delegate = new ValidCarTypeValidator();

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
