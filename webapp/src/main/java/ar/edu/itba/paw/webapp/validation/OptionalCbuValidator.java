package ar.edu.itba.paw.webapp.validation;

import java.util.Locale;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import ar.edu.itba.paw.models.util.CbuRules;
import ar.edu.itba.paw.webapp.validation.constraint.OptionalCbu;

@Component
public final class OptionalCbuValidator implements ConstraintValidator<OptionalCbu, String> {

    private final MessageSource messageSource;

    public OptionalCbuValidator(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        if (CbuRules.isValidFormat(value)) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        final Locale locale = LocaleContextHolder.getLocale();
        final String msg = messageSource.getMessage(
                "profile.cbu.size",
                new Object[] { CbuRules.REQUIRED_DIGIT_LENGTH },
                locale);
        context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
        return false;
    }
}
