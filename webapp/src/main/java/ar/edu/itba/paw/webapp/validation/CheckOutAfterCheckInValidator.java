package ar.edu.itba.paw.webapp.validation;

import java.util.Locale;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.form.PublishCarForm;

@Component
public class CheckOutAfterCheckInValidator implements ConstraintValidator<CheckOutAfterCheckIn, PublishCarForm> {

    private final MessageSource messageSource;

    @Autowired
    public CheckOutAfterCheckInValidator(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public boolean isValid(final PublishCarForm form, final ConstraintValidatorContext context) {
        if (form == null) {
            return true;
        }
        if (form.getCheckInTime() == null || form.getCheckOutTime() == null) {
            return true;
        }
        if (form.getCheckOutTime().isAfter(form.getCheckInTime())) {
            return true;
        }
        final Locale locale = LocaleContextHolder.getLocale();
        final String msg = messageSource.getMessage("validation.checkOutTime.afterCheckIn", null, locale);
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(msg)
                .addPropertyNode("checkOutTime")
                .addConstraintViolation();
        return false;
    }
}
