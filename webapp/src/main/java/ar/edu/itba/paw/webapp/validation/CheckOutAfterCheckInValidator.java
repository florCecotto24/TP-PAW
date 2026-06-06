package ar.edu.itba.paw.webapp.validation;

import java.util.Locale;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.policy.CarAvailabilityCheckInOutPolicy;
import ar.edu.itba.paw.webapp.form.CarAvailabilityTimeWindow;
import ar.edu.itba.paw.webapp.validation.constraint.CheckOutAfterCheckIn;

/** Bean Validation engine for {@link CheckOutAfterCheckIn} on {@link CarAvailabilityTimeWindow} forms. */
@Component
public final class CheckOutAfterCheckInValidator implements ConstraintValidator<CheckOutAfterCheckIn, CarAvailabilityTimeWindow> {

    private final MessageSource messageSource;
    private final CarAvailabilityCheckInOutPolicy carAvailabilityCheckInOutPolicy;

    public CheckOutAfterCheckInValidator(
            final MessageSource messageSource,
            final CarAvailabilityCheckInOutPolicy carAvailabilityCheckInOutPolicy) {
        this.messageSource = messageSource;
        this.carAvailabilityCheckInOutPolicy = carAvailabilityCheckInOutPolicy;
    }

    @Override
    public boolean isValid(final CarAvailabilityTimeWindow form, final ConstraintValidatorContext context) {
        if (form == null) {
            return true;
        }
        if (form.getCheckInTime() == null || form.getCheckOutTime() == null) {
            return true;
        }
        if (!form.getCheckOutTime().isAfter(form.getCheckInTime())) {
            final Locale locale = LocaleContextHolder.getLocale();
            final String msg = messageSource.getMessage("validation.checkOutTime.afterCheckIn", null, locale);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(msg)
                    .addPropertyNode("checkOutTime")
                    .addConstraintViolation();
            return false;
        }
        if (!carAvailabilityCheckInOutPolicy.hasMinimumGap(form.getCheckInTime(), form.getCheckOutTime())) {
            final Locale locale = LocaleContextHolder.getLocale();
            final String msg = messageSource.getMessage(
                    MessageKeys.CAR_AVAILABILITY_CHECKINOUT_MIN_GAP,
                    new Object[] {carAvailabilityCheckInOutPolicy.getMinHoursBetweenCheckInAndCheckOut()},
                    locale);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(msg)
                    .addPropertyNode("checkOutTime")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
