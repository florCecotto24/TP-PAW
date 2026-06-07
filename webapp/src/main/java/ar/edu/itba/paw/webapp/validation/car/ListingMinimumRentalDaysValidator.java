package ar.edu.itba.paw.webapp.validation.car;

import java.util.Locale;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.policy.ListingFormValidationPolicy;
import ar.edu.itba.paw.webapp.validation.constraint.car.ListingMinimumRentalDays;

/**
 * Bean Validation entry point for {@link ListingMinimumRentalDays}. Fetches the configured
 * range from {@link ListingFormValidationPolicy}.
 */
@Component
public final class ListingMinimumRentalDaysValidator
        implements ConstraintValidator<ListingMinimumRentalDays, Integer> {

    private final ListingFormValidationPolicy policy;
    private final MessageSource messageSource;

    private String messageKey;

    public ListingMinimumRentalDaysValidator(
            final ListingFormValidationPolicy policy, final MessageSource messageSource) {
        this.policy = policy;
        this.messageSource = messageSource;
    }

    @Override
    public void initialize(final ListingMinimumRentalDays constraintAnnotation) {
        this.messageKey = constraintAnnotation.messageKey();
    }

    @Override
    public boolean isValid(final Integer value, final ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        final int min = policy.getMinimumRentalDaysMin();
        final int max = policy.getMinimumRentalDaysMax();
        if (value >= min && value <= max) {
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
