package ar.edu.itba.paw.webapp.validation.car;

import java.util.Collection;
import java.util.Locale;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.policy.ListingFormValidationPolicy;
import ar.edu.itba.paw.webapp.validation.constraint.car.ListingAvailabilityRowsSize;

/**
 * Bean Validation entry point for {@link ListingAvailabilityRowsSize}. Fetches the
 * configured min/max from {@link ListingFormValidationPolicy} so all listing form usages stay
 * consistent.
 */
@Component
public final class ListingAvailabilityRowsSizeValidator
        implements ConstraintValidator<ListingAvailabilityRowsSize, Collection<?>> {

    private final ListingFormValidationPolicy policy;
    private final MessageSource messageSource;

    private boolean enforceMinimum;
    private String messageKey;

    public ListingAvailabilityRowsSizeValidator(
            final ListingFormValidationPolicy policy, final MessageSource messageSource) {
        this.policy = policy;
        this.messageSource = messageSource;
    }

    @Override
    public void initialize(final ListingAvailabilityRowsSize constraintAnnotation) {
        this.enforceMinimum = constraintAnnotation.enforceMinimum();
        this.messageKey = constraintAnnotation.messageKey();
    }

    @Override
    public boolean isValid(final Collection<?> value, final ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        final int min = enforceMinimum ? policy.getAvailabilityRowsMin() : 0;
        final int max = policy.getAvailabilityRowsMax();
        final int size = value.size();
        if (size >= min && size <= max) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        final Locale locale = LocaleContextHolder.getLocale();
        final String template = messageSource.getMessage(messageKey, new Object[] {}, messageKey, locale);
        final String resolved = template
                .replace("{min}", Integer.toString(min))
                .replace("{max}", Integer.toString(max));
        context.buildConstraintViolationWithTemplate(resolved).addConstraintViolation();
        return false;
    }
}
