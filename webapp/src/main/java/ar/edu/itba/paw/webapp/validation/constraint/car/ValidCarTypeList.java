package ar.edu.itba.paw.webapp.validation.constraint.car;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.car.ValidCarTypeListValidator;

/** Every list element must be a valid REST car type token. */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidCarTypeListValidator.class)
public @interface ValidCarTypeList {

    String message() default "{validation.type.unknown}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
