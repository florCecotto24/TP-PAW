package ar.edu.itba.paw.webapp.validation.constraint.reservation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.reservation.ReservationPatchDatesTogetherValidator;

/** {@code startDate} and {@code endDate} must be patched together or not at all. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ReservationPatchDatesTogetherValidator.class)
public @interface ReservationPatchDatesTogether {

    String message() default "{validation.reservation.patch.datesTogether}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
