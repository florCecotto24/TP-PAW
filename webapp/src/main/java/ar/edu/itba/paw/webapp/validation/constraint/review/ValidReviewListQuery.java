package ar.edu.itba.paw.webapp.validation.constraint.review;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.review.ValidReviewListQueryValidator;

/** {@code GET /reviews} must filter by exactly one of carId, recipientUserId, or reservationId. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidReviewListQueryValidator.class)
public @interface ValidReviewListQuery {

    String message() default "{validation.review.list.filter.required}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
