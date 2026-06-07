package ar.edu.itba.paw.webapp.validation.constraint.user;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.user.ValidCbuValidator;

/**
 * Strict CBU constraint: the value MUST be a valid Argentine CBU (non-null, non-blank, exact digit length).
 * Use {@link OptionalCbu} when blank means "leave unchanged".
 */
@Documented
@Constraint(validatedBy = ValidCbuValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCbu {

    String message() default "{profile.cbu.size}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
