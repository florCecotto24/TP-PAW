package ar.edu.itba.paw.webapp.validation.constraint.user;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.user.OptionalCbuValidator;

/**
 * Optional CBU on profile PATCH: {@code null}/absent leaves the stored value unchanged; blank/empty
 * clears it (service pauses cars that require a CBU). When non-blank, must pass Argentine CBU checks.
 */
@Documented
@Constraint(validatedBy = OptionalCbuValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface OptionalCbu {

    String message() default "{profile.cbu.size}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
