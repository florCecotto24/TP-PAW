package ar.edu.itba.paw.webapp.validation.constraint.user;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.user.ValidProfilePicturePayloadValidator;

/** REST profile-picture upload: non-empty image within configured size limits. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidProfilePicturePayloadValidator.class)
public @interface ValidProfilePicturePayload {

    String message() default "{profile.picture.notImage}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
