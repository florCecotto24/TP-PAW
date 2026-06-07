package ar.edu.itba.paw.webapp.validation.constraint.reservation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.reservation.CheckOutAfterCheckInValidator;

/** Type-level check: listing check-out wall time is strictly after check-in (and minimum gap when configured). */
@Documented
@Constraint(validatedBy = CheckOutAfterCheckInValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckOutAfterCheckIn {

    String message() default "{validation.checkOutTime.afterCheckIn}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
