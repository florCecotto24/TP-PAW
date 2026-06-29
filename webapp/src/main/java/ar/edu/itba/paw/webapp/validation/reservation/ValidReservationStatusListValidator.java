package ar.edu.itba.paw.webapp.validation.reservation;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.validation.constraint.reservation.ValidReservationStatusList;

public final class ValidReservationStatusListValidator
        implements ConstraintValidator<ValidReservationStatusList, List<String>> {

    private final ValidReservationStatusTokenValidator delegate = new ValidReservationStatusTokenValidator();

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
