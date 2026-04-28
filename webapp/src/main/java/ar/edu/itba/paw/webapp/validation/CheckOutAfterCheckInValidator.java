package ar.edu.itba.paw.webapp.validation;

import java.util.Locale;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.services.policy.ListingCheckInOutPolicy;
import ar.edu.itba.paw.webapp.form.ListingTimeWindow;
import ar.edu.itba.paw.webapp.validation.constraint.CheckOutAfterCheckIn;

@Component
public class CheckOutAfterCheckInValidator implements ConstraintValidator<CheckOutAfterCheckIn, ListingTimeWindow> {

    private final MessageSource messageSource;
    private final ListingCheckInOutPolicy listingCheckInOutPolicy;

    @Autowired
    public CheckOutAfterCheckInValidator(
            final MessageSource messageSource,
            final ListingCheckInOutPolicy listingCheckInOutPolicy) {
        this.messageSource = messageSource;
        this.listingCheckInOutPolicy = listingCheckInOutPolicy;
    }

    @Override
    public boolean isValid(final ListingTimeWindow form, final ConstraintValidatorContext context) {
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
        if (!listingCheckInOutPolicy.hasMinimumGap(form.getCheckInTime(), form.getCheckOutTime())) {
            final Locale locale = LocaleContextHolder.getLocale();
            final String msg = messageSource.getMessage(
                    MessageKeys.LISTING_CHECKINOUT_MIN_GAP,
                    new Object[] {listingCheckInOutPolicy.getMinHoursBetweenCheckInAndCheckOut()},
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
