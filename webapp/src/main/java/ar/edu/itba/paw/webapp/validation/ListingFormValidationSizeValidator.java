package ar.edu.itba.paw.webapp.validation;

import java.util.Locale;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.policy.ListingFormValidationPolicy;
import ar.edu.itba.paw.webapp.validation.constraint.ListingFormValidationSize;
import ar.edu.itba.paw.webapp.validation.constraint.ListingFormValidationSize.Kind;

@Component
public final class ListingFormValidationSizeValidator
        implements ConstraintValidator<ListingFormValidationSize, String> {

    private final ListingFormValidationPolicy policy;
    private final MessageSource messageSource;

    private Kind kind;
    private String messageKey;

    public ListingFormValidationSizeValidator(
            final ListingFormValidationPolicy policy, final MessageSource messageSource) {
        this.policy = policy;
        this.messageSource = messageSource;
    }

    @Override
    public void initialize(final ListingFormValidationSize constraintAnnotation) {
        this.kind = constraintAnnotation.kind();
        this.messageKey = constraintAnnotation.messageKey();
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        final int max = switch (kind) {
            case ADDRESS_STREET -> policy.getAddressStreetMaxLength();
            case ADDRESS_NUMBER -> policy.getAddressNumberMaxLength();
        };
        if (value.length() <= max) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        final Locale locale = LocaleContextHolder.getLocale();
        final String msg = messageSource.getMessage(messageKey, new Object[] { max }, locale);
        context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
        return false;
    }
}
