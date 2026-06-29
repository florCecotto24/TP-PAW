package ar.edu.itba.paw.webapp.validation.constraint.reservation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.reservation.ValidReservationPatchStatusValidator;

/** PATCH status must be a participant cancellation token when present. */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidReservationPatchStatusValidator.class)
public @interface ValidReservationPatchStatus {

    String message() default "{validation.reservation.patch.statusUnsupported}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
