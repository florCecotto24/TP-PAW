package ar.edu.itba.paw.webapp.validation.car;

import java.time.LocalDate;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.form.car.AvailabilityCreateForm;
import ar.edu.itba.paw.webapp.validation.constraint.car.EndDateOnOrAfterStartDate;

public final class EndDateOnOrAfterStartDateValidator
        implements ConstraintValidator<EndDateOnOrAfterStartDate, AvailabilityCreateForm> {

    @Override
    public boolean isValid(final AvailabilityCreateForm form, final ConstraintValidatorContext context) {
        if (form == null) {
            return true;
        }
        final LocalDate start = form.getStartDate();
        final LocalDate end = form.getEndDate();
        if (start == null || end == null) {
            return true;
        }
        if (end.isBefore(start)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("endDate")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
