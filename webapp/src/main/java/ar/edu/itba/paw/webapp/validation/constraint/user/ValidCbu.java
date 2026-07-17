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
 * For PATCH profile forms use {@link OptionalCbu}: blank/empty clears the stored CBU (and may pause
 * listings that require one); omit the field entirely to leave it unchanged.
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
