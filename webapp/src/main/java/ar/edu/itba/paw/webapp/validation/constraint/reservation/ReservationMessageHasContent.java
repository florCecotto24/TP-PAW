package ar.edu.itba.paw.webapp.validation.constraint.reservation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.reservation.ReservationMessageHasContentValidator;

/** Chat message requires non-blank body and/or an attachment. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ReservationMessageHasContentValidator.class)
public @interface ReservationMessageHasContent {

    String message() default "{validation.reservation.message.empty}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
