package ar.edu.itba.paw.webapp.validation.constraint;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.NoPunctuationValidator;

@Documented
@Constraint(validatedBy = NoPunctuationValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoPunctuation {
    String message() default "{validation.noPunctuation}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
