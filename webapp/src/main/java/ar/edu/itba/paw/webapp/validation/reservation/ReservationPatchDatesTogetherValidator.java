package ar.edu.itba.paw.webapp.validation.reservation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.form.reservation.ReservationPatchForm;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.ReservationPatchDatesTogether;

public final class ReservationPatchDatesTogetherValidator
        implements ConstraintValidator<ReservationPatchDatesTogether, ReservationPatchForm> {

    @Override
    public boolean isValid(final ReservationPatchForm form, final ConstraintValidatorContext context) {
        if (form == null) {
            return true;
        }
        final boolean hasStart = form.getStartDate() != null && !form.getStartDate().isBlank();
        final boolean hasEnd = form.getEndDate() != null && !form.getEndDate().isBlank();
        return hasStart == hasEnd;
    }
}
