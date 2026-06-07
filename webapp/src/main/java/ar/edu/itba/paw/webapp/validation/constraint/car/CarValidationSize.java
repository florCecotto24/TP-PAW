package ar.edu.itba.paw.webapp.validation.constraint.car;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.car.CarValidationSizeValidator;

/**
 * Field length bound derived at runtime from {@code CarValidationPolicy} for the chosen {@link Kind}.
 * Both the minimum and the maximum live in {@code application.properties} so there are no magic numbers in forms.
 */
@Documented
@Constraint(validatedBy = CarValidationSizeValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CarValidationSize {

    Kind kind();

    /** Key in {@code messages*.properties}; interpolated with {@code {0}=min}, {@code {1}=max}. */
    String messageKey();

    /** Unused at runtime (validator builds the message); required non-empty for Bean Validation. */
    String message() default "{javax.validation.constraints.Size.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    enum Kind {
        BRAND,
        MODEL,
        PLATE,
        DESCRIPTION
    }
}
