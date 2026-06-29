package ar.edu.itba.paw.webapp.validation.car;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarPowertrainList;

public final class ValidCarPowertrainListValidator implements ConstraintValidator<ValidCarPowertrainList, List<String>> {

    private final ValidCarPowertrainValidator delegate = new ValidCarPowertrainValidator();

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
