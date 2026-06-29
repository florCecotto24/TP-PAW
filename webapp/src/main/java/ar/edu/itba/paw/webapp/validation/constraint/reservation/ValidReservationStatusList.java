package ar.edu.itba.paw.webapp.validation.constraint.reservation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.reservation.ValidReservationStatusListValidator;

@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidReservationStatusListValidator.class)
public @interface ValidReservationStatusList {

    String message() default "{validation.reservation.status.unknown}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
