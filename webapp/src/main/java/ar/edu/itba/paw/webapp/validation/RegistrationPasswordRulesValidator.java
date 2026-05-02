package ar.edu.itba.paw.webapp.validation;

import java.util.Locale;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import ar.edu.itba.paw.services.policy.UserValidationPolicy;
import ar.edu.itba.paw.webapp.form.RegistrationPasswordConfirmFields;
import ar.edu.itba.paw.webapp.validation.constraint.RegistrationPasswordRules;

/** Bean Validation engine for {@link RegistrationPasswordRules} on password pair forms. */
@Component
public final class RegistrationPasswordRulesValidator implements ConstraintValidator<RegistrationPasswordRules, RegistrationPasswordConfirmFields> {

    private final UserValidationPolicy policy;
    private final MessageSource messageSource;

    public RegistrationPasswordRulesValidator(final UserValidationPolicy policy, final MessageSource messageSource) {
        this.policy = policy;
        this.messageSource = messageSource;
    }

    @Override
    public boolean isValid(final RegistrationPasswordConfirmFields form, final ConstraintValidatorContext context) {
        if (form == null) {
            return true;
        }
        final String p = form.getPassword();
        final String c = form.getPasswordConfirm();
        if (!StringUtils.hasText(p) || !StringUtils.hasText(c)) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        boolean ok = true;
        final Locale locale = LocaleContextHolder.getLocale();
        if (p.length() < policy.getRegistrationPasswordMinLength()) {
            final String msg = messageSource.getMessage(
                    "validation.registration.password.minLength",
                    new Object[] { policy.getRegistrationPasswordMinLength() },
                    locale);
            context.buildConstraintViolationWithTemplate(msg).addPropertyNode("password").addConstraintViolation();
            ok = false;
        }
        if (p.length() > policy.getRegistrationPasswordMaxLength()) {
            final String msg = messageSource.getMessage(
                    "validation.registration.password.maxLength",
                    new Object[] { policy.getRegistrationPasswordMaxLength() },
                    locale);
            context.buildConstraintViolationWithTemplate(msg).addPropertyNode("password").addConstraintViolation();
            ok = false;
        }
        if (!p.equals(c)) {
            final String msg = messageSource.getMessage("validation.registration.password.mismatch", null, locale);
            context.buildConstraintViolationWithTemplate(msg).addPropertyNode("passwordConfirm").addConstraintViolation();
            ok = false;
        }
        return ok;
    }
}
