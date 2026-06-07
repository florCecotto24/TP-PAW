package ar.edu.itba.paw.webapp.validation;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.policy.ListingFormValidationPolicy;
import ar.edu.itba.paw.webapp.validation.constraint.ListingPricePerDay;

/**
 * Bean Validation entry point for {@link ListingPricePerDay}. Performs both the minimum-value
 * and digit-precision checks against the values exposed by {@link ListingFormValidationPolicy}.
 */
@Component
public final class ListingPricePerDayValidator
        implements ConstraintValidator<ListingPricePerDay, BigDecimal> {

    private final ListingFormValidationPolicy policy;
    private final MessageSource messageSource;

    private String belowMinMessageKey;
    private String digitsMessageKey;

    public ListingPricePerDayValidator(
            final ListingFormValidationPolicy policy, final MessageSource messageSource) {
        this.policy = policy;
        this.messageSource = messageSource;
    }

    @Override
    public void initialize(final ListingPricePerDay constraintAnnotation) {
        this.belowMinMessageKey = constraintAnnotation.belowMinMessageKey();
        this.digitsMessageKey = constraintAnnotation.digitsMessageKey();
    }

    @Override
    public boolean isValid(final BigDecimal value, final ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        final BigDecimal min = policy.getPricePerDayMin();
        if (value.compareTo(min) < 0) {
            return reject(context, belowMinMessageKey, Map.of("min", min.toPlainString()));
        }
        final int integerDigits = policy.getPricePerDayIntegerDigits();
        final int fractionDigits = policy.getPricePerDayFractionDigits();
        final BigDecimal stripped = value.stripTrailingZeros();
        final int actualIntegerDigits = stripped.precision() - stripped.scale();
        final int actualFractionDigits = Math.max(0, stripped.scale());
        if (actualIntegerDigits > integerDigits || actualFractionDigits > fractionDigits) {
            return reject(context, digitsMessageKey,
                    Map.of("integer", integerDigits, "fraction", fractionDigits));
        }
        return true;
    }

    private boolean reject(
            final ConstraintValidatorContext context,
            final String messageKey,
            final Map<String, Object> placeholders) {
        context.disableDefaultConstraintViolation();
        final Locale locale = LocaleContextHolder.getLocale();
        final String template = messageSource.getMessage(messageKey, new Object[] {}, messageKey, locale);
        final String resolved = applyPlaceholders(template, placeholders);
        context.buildConstraintViolationWithTemplate(resolved).addConstraintViolation();
        return false;
    }

    private static String applyPlaceholders(final String template, final Map<String, Object> placeholders) {
        if (template == null) {
            return "";
        }
        String result = template;
        // Bean Validation uses {key} placeholders too; we resolve them by simple replacement so
        // both the legacy hardcoded messages and the new {min}/{integer}/{fraction} ones work.
        final Map<String, Object> safe = new HashMap<>(placeholders);
        for (final Map.Entry<String, Object> entry : safe.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }
}
