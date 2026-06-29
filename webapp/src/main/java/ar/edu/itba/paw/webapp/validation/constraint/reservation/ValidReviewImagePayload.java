package ar.edu.itba.paw.webapp.validation.constraint.reservation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.reservation.ValidReviewImagePayloadValidator;

/** Optional review image must be a supported image type when bytes are present. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidReviewImagePayloadValidator.class)
public @interface ValidReviewImagePayload {

    String message() default "{profile.picture.notImage}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
