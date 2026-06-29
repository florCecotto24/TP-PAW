package ar.edu.itba.paw.webapp.validation.reservation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.webapp.support.ReservationRestEnums;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.ValidReservationPatchStatus;

public final class ValidReservationPatchStatusValidator
        implements ConstraintValidator<ValidReservationPatchStatus, String> {

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            final Reservation.Status status = ReservationRestEnums.parseStatus(value);
            return status == Reservation.Status.CANCELLED_BY_RIDER
                    || status == Reservation.Status.CANCELLED_BY_OWNER;
        } catch (final IllegalArgumentException ex) {
            return false;
        }
    }
}
