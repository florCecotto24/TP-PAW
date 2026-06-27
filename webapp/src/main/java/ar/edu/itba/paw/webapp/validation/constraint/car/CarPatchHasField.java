package ar.edu.itba.paw.webapp.validation.constraint.car;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.car.CarPatchHasFieldValidator;

/** PATCH body must include at least one mutable field (openapi partial update). */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CarPatchHasFieldValidator.class)
public @interface CarPatchHasField {

    String message() default "{validation.car.patch.empty}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
