package ar.edu.itba.paw.webapp.validation.constraint.user;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.user.ValidUserRoleValidator;

/** Admin user role token ({@code admin} or {@code user}) when present. */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidUserRoleValidator.class)
public @interface ValidUserRole {

    String message() default "{validation.role.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
