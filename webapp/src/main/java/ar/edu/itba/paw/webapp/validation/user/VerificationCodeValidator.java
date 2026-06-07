package ar.edu.itba.paw.webapp.validation.user;

import java.util.Locale;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.policy.VerificationCodePolicy;
import ar.edu.itba.paw.webapp.validation.constraint.user.VerificationCode;

/** Engine for {@link VerificationCode}: null/blank passes (let {@code @NotBlank} produce its own error). */
@Component
public final class VerificationCodeValidator implements ConstraintValidator<VerificationCode, String> {

    private final VerificationCodePolicy policy;
    private final MessageSource messageSource;

    private String messageKey;

    public VerificationCodeValidator(final VerificationCodePolicy policy, final MessageSource messageSource) {
        this.policy = policy;
        this.messageSource = messageSource;
    }

    @Override
    public void initialize(final VerificationCode constraintAnnotation) {
        this.messageKey = constraintAnnotation.messageKey();
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        if (policy.getCodePattern().matcher(value).matches()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        final Locale locale = LocaleContextHolder.getLocale();
        final String msg =
                messageSource.getMessage(messageKey, new Object[] { policy.getCodeLength() }, locale);
        context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
        return false;
    }
}
