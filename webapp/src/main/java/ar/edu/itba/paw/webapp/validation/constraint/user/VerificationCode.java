package ar.edu.itba.paw.webapp.validation.constraint.user;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.user.VerificationCodeValidator;

/**
 * Validates that the value is a digits-only string whose length matches
 * {@link ar.edu.itba.paw.policy.VerificationCodePolicy#getCodeLength()}.
 * The pattern and length come from {@code app.validation.verification-code-length}, never hardcoded.
 */
@Documented
@Constraint(validatedBy = VerificationCodeValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface VerificationCode {

    /** Key in {@code messages*.properties}; interpolated with {@code {0}=code length}. */
    String messageKey() default "forgotPassword.code.pattern";

    String message() default "{javax.validation.constraints.Pattern.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
