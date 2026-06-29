package ar.edu.itba.paw.webapp.validation.constraint.car;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.car.ValidCarTransmissionListValidator;

@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidCarTransmissionListValidator.class)
public @interface ValidCarTransmissionList {

    String message() default "{validation.transmission.unknown}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
