package ar.edu.itba.paw.webapp.validation.constraint.car;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.car.ValidCarTypeValidator;

/** REST car type token must match {@link ar.edu.itba.paw.models.domain.car.Car.Type}. */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidCarTypeValidator.class)
public @interface ValidCarType {

    String message() default "{validation.type.unknown}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
