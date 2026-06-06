package ar.edu.itba.paw.webapp.validation;

import java.util.Locale;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.policy.CarValidationPolicy;
import ar.edu.itba.paw.webapp.validation.constraint.CarValidationSize;
import ar.edu.itba.paw.webapp.validation.constraint.CarValidationSize.Kind;

/**
 * Engine for {@link CarValidationSize}: looks up the {@link Kind}-specific min/max in {@link CarValidationPolicy}
 * and resolves the violation message through {@link MessageSource}. Null/blank input is ignored so {@code @NotBlank}
 * can keep producing its own message independently.
 */
@Component
public final class CarValidationSizeValidator implements ConstraintValidator<CarValidationSize, String> {

    private final CarValidationPolicy policy;
    private final MessageSource messageSource;

    private Kind kind;
    private String messageKey;

    public CarValidationSizeValidator(final CarValidationPolicy policy, final MessageSource messageSource) {
        this.policy = policy;
        this.messageSource = messageSource;
    }

    @Override
    public void initialize(final CarValidationSize constraintAnnotation) {
        this.kind = constraintAnnotation.kind();
        this.messageKey = constraintAnnotation.messageKey();
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        final int min = switch (kind) {
            case BRAND -> policy.getBrandMinLength();
            case PLATE -> policy.getPlateMinLength();
            case MODEL, DESCRIPTION -> 0;
        };
        final int max = switch (kind) {
            case BRAND -> policy.getBrandMaxLength();
            case MODEL -> policy.getModelMaxLength();
            case PLATE -> policy.getPlateMaxLength();
            case DESCRIPTION -> policy.getDescriptionMaxLength();
        };
        final int length = value.trim().length();
        if (length >= min && length <= max) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        final Locale locale = LocaleContextHolder.getLocale();
        final String msg = messageSource.getMessage(messageKey, new Object[] { min, max }, locale);
        context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
        return false;
    }
}
