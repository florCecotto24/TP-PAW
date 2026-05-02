package ar.edu.itba.paw.webapp.validation.constraint;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.ReservationWithinMaxBillableDaysValidator;

/** Type-level check: parsed reservation interval does not exceed configured max billable days. */
@Documented
@Constraint(validatedBy = ReservationWithinMaxBillableDaysValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ReservationWithinMaxBillableDays {

    String message() default "{validation.reservationForm.maxBillableDays}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
