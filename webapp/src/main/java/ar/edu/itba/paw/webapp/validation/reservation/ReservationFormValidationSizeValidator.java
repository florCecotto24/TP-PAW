package ar.edu.itba.paw.webapp.validation.reservation;

import java.util.Locale;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.policy.ReservationFormValidationPolicy;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.ReservationFormValidationSize.Kind;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.ReservationFormValidationSize;

@Component
public final class ReservationFormValidationSizeValidator
        implements ConstraintValidator<ReservationFormValidationSize, String> {

    private final ReservationFormValidationPolicy policy;
    private final MessageSource messageSource;

    private Kind kind;
    private String messageKey;

    public ReservationFormValidationSizeValidator(
            final ReservationFormValidationPolicy policy, final MessageSource messageSource) {
        this.policy = policy;
        this.messageSource = messageSource;
    }

    @Override
    public void initialize(final ReservationFormValidationSize constraintAnnotation) {
        this.kind = constraintAnnotation.kind();
        this.messageKey = constraintAnnotation.messageKey();
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        final int max = switch (kind) {
            case DELIVERY_LOCATION -> policy.getDeliveryLocationMaxLength();
            case CAR_NAME -> policy.getCarNameMaxLength();
            case DATETIME_INPUT -> policy.getDatetimeInputMaxLength();
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
