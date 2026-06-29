package ar.edu.itba.paw.webapp.validation.constraint.reservation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.reservation.ReservationAvailabilityMatchesCarValidator;

/** {@code availabilityUri} must reference an availability under the same car as {@code carUri}. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ReservationAvailabilityMatchesCarValidator.class)
public @interface ReservationAvailabilityMatchesCar {

    String message() default "{validation.reservation.availabilityCarMismatch}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
