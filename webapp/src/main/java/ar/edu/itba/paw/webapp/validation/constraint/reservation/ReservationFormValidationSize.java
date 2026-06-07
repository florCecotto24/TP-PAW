package ar.edu.itba.paw.webapp.validation.constraint.reservation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.reservation.ReservationFormValidationSizeValidator;

/**
 * Length bound resolved at runtime against {@link ar.edu.itba.paw.policy.ReservationFormValidationPolicy}
 * (delivery location, display car name, hidden datetime strings).
 */
@Documented
@Constraint(validatedBy = ReservationFormValidationSizeValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ReservationFormValidationSize {

    Kind kind();

    /** Key in {@code messages*.properties}; interpolated with {@code {0}=max}. */
    String messageKey();

    String message() default "{javax.validation.constraints.Size.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    enum Kind {
        DELIVERY_LOCATION,
        CAR_NAME,
        DATETIME_INPUT
    }
}
