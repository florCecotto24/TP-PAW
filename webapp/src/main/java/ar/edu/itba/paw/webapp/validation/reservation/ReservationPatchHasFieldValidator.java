package ar.edu.itba.paw.webapp.validation.reservation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.form.reservation.ReservationPatchForm;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.ReservationPatchHasField;

public final class ReservationPatchHasFieldValidator
        implements ConstraintValidator<ReservationPatchHasField, ReservationPatchForm> {

    @Override
    public boolean isValid(final ReservationPatchForm form, final ConstraintValidatorContext context) {
        if (form == null) {
            return false;
        }
        return (form.getStatus() != null && !form.getStatus().isBlank())
                || Boolean.TRUE.equals(form.getCarReturned())
                || form.getStartDate() != null
                || form.getEndDate() != null;
    }
}
