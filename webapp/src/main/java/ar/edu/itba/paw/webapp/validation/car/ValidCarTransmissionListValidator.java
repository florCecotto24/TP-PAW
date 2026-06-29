package ar.edu.itba.paw.webapp.validation.car;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarTransmissionList;

public final class ValidCarTransmissionListValidator
        implements ConstraintValidator<ValidCarTransmissionList, List<String>> {

    private final ValidCarTransmissionValidator delegate = new ValidCarTransmissionValidator();

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
