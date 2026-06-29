package ar.edu.itba.paw.webapp.validation.constraint.car;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.car.EndDateOnOrAfterStartDateValidator;

/** Availability period end date must be on or after the start date. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EndDateOnOrAfterStartDateValidator.class)
public @interface EndDateOnOrAfterStartDate {

    String message() default "{validation.availabilityRow.endBeforeStart}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
