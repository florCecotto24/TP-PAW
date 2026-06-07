package ar.edu.itba.paw.webapp.validation.constraint.user;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.user.OptionalCbuValidator;

/** When non-blank, value must be a valid Argentine CBU length and digit checksum. */
@Documented
@Constraint(validatedBy = OptionalCbuValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface OptionalCbu {

    String message() default "{profile.cbu.size}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
