package ar.edu.itba.paw.webapp.validation.constraint.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.common.ValidYearMonthValidator;

/** ISO-8601 year-month ({@code yyyy-MM}) when present. */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidYearMonthValidator.class)
public @interface ValidYearMonth {

    String message() default "{validation.yearMonth.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
