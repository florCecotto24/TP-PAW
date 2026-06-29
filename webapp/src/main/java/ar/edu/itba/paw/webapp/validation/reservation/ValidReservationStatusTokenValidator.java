package ar.edu.itba.paw.webapp.validation.reservation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.support.ReservationRestEnums;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.ValidReservationStatusToken;

public final class ValidReservationStatusTokenValidator
        implements ConstraintValidator<ValidReservationStatusToken, String> {

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            ReservationRestEnums.parseStatus(value);
            return true;
        } catch (final IllegalArgumentException ex) {
            return false;
        }
    }
}
