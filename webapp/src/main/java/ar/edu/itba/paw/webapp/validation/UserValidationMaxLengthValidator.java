package ar.edu.itba.paw.webapp.validation;

import java.util.Locale;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import ar.edu.itba.paw.services.policy.UserValidationPolicy;
import ar.edu.itba.paw.webapp.validation.constraint.UserValidationMaxLength;
import ar.edu.itba.paw.webapp.validation.constraint.UserValidationMaxLength.Kind;

/** Bean Validation engine for {@link UserValidationMaxLength} using policy limits and {@link MessageSource}. */
@Component
public final class UserValidationMaxLengthValidator implements ConstraintValidator<UserValidationMaxLength, String> {

    private final UserValidationPolicy policy;
    private final MessageSource messageSource;

    private Kind kind;
    private String messageKey;

    public UserValidationMaxLengthValidator(final UserValidationPolicy policy, final MessageSource messageSource) {
        this.policy = policy;
        this.messageSource = messageSource;
    }

    @Override
    public void initialize(final UserValidationMaxLength constraintAnnotation) {
        this.kind = constraintAnnotation.kind();
        this.messageKey = constraintAnnotation.messageKey();
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        final int max = switch (kind) {
            case DISPLAY_NAME_PART -> policy.getDisplayNamePartMaxLength();
            case REGISTRATION_EMAIL -> policy.getRegistrationEmailMaxLength();
            case PROFILE_ABOUT -> policy.getProfileAboutMaxLength();
        };
        final String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        final Locale locale = LocaleContextHolder.getLocale();
        final String msg = messageSource.getMessage(messageKey, new Object[] { max }, locale);
        context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
        return false;
    }
}
