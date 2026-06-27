package ar.edu.itba.paw.webapp.validation.constraint.reservation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.reservation.ReservationPatchHasFieldValidator;

/** PATCH body must include at least one mutable field (openapi partial update). */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ReservationPatchHasFieldValidator.class)
public @interface ReservationPatchHasField {

    String message() default "{validation.reservation.patch.empty}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
