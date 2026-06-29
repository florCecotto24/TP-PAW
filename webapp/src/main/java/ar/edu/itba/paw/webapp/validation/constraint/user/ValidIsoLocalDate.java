package ar.edu.itba.paw.webapp.validation.constraint.user;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.user.ValidIsoLocalDateValidator;

/** Optional ISO-8601 calendar date ({@code yyyy-MM-dd}) when present. */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidIsoLocalDateValidator.class)
public @interface ValidIsoLocalDate {

    String message() default "{profile.birthDate.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
