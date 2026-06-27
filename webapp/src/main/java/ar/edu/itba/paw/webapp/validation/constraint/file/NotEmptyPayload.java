package ar.edu.itba.paw.webapp.validation.constraint.file;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.file.NotEmptyPayloadValidator;

/** Binary upload must contain at least one byte. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotEmptyPayloadValidator.class)
public @interface NotEmptyPayload {

    String message() default "{validation.file.empty}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
