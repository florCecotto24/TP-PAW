package ar.edu.itba.paw.webapp.validation;

import java.util.Locale;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import ar.edu.itba.paw.services.policy.UserValidationPolicy;
import ar.edu.itba.paw.webapp.validation.constraint.PhoneNumber;

/** Bean Validation engine for {@link PhoneNumber} against {@link UserValidationPolicy}. */
@Component
public final class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

    private final UserValidationPolicy policy;
    private final MessageSource messageSource;

    public PhoneNumberValidator(final UserValidationPolicy policy, final MessageSource messageSource) {
        this.policy = policy;
        this.messageSource = messageSource;
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        final String trimmed = value.trim();
        final Locale locale = LocaleContextHolder.getLocale();
        if (trimmed.length() > policy.getProfilePhoneMaxLength()
                || !policy.getProfilePhonePattern().matcher(trimmed).matches()) {
            context.disableDefaultConstraintViolation();
            final String msg = messageSource.getMessage(
                    "validation.profile.phone.invalid",
                    new Object[] { policy.getProfilePhoneMaxLength() },
                    locale);
            context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
            return false;
        }
        return true;
    }
}
