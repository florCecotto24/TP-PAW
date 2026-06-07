package ar.edu.itba.paw.webapp.validation.constraint.user;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.user.UserValidationMaxLengthValidator;

/** Field max length derived from {@code UserValidationPolicy} for the chosen {@link Kind}. */
@Documented
@Constraint(validatedBy = UserValidationMaxLengthValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface UserValidationMaxLength {

    Kind kind();

    /** Key in {@code messages*.properties} (interpolated with max length as single argument). */
    String messageKey();

    /** Unused at runtime (validator builds the message); required non-empty for Bean Validation. */
    String message() default "{javax.validation.constraints.Size.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    enum Kind {
        DISPLAY_NAME_PART,
        REGISTRATION_EMAIL,
        PROFILE_ABOUT
    }
}
