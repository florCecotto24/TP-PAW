package ar.edu.itba.paw.webapp.validation.constraint.car;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.car.ValidCarStatusListValidator;

/** Every list element must be a valid REST car status token. */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidCarStatusListValidator.class)
public @interface ValidCarStatusList {

    String message() default "{validation.car.status.unknown}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
