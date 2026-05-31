package ar.edu.itba.paw.webapp.validation;

import java.util.Locale;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.util.rules.CbuRules;
import ar.edu.itba.paw.webapp.validation.constraint.ValidCbu;

/**
 * Bean Validation engine for {@link ValidCbu}: rejects null, blank or malformed CBUs by delegating
 * the digit-length check to {@link CbuRules#isValidFormat(String)}. The accompanying message is resolved
 * against the application {@link MessageSource} using the request locale so JSP error tags render the
 * localized text without callers having to plumb {@link MessageSource} manually.
 */
@Component
public final class ValidCbuValidator implements ConstraintValidator<ValidCbu, String> {

    private final MessageSource messageSource;

    public ValidCbuValidator(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
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
