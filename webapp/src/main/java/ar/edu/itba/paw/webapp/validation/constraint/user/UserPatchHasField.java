package ar.edu.itba.paw.webapp.validation.constraint.user;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.user.UserPatchHasFieldValidator;

/** PATCH body must include at least one mutable field (openapi partial update). */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UserPatchHasFieldValidator.class)
public @interface UserPatchHasField {

    String message() default "{validation.user.patch.empty}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
