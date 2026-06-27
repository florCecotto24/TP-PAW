package ar.edu.itba.paw.webapp.validation.reservation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.form.reservation.ReservationMessageCreateForm;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.ReservationMessageHasContent;

public final class ReservationMessageHasContentValidator
        implements ConstraintValidator<ReservationMessageHasContent, ReservationMessageCreateForm> {

    @Override
    public boolean isValid(final ReservationMessageCreateForm form, final ConstraintValidatorContext context) {
        if (form == null) {
            return false;
        }
        if (form.isHasAttachment()) {
            return true;
        }
        return form.getBody() != null && !form.getBody().isBlank();
    }
}
