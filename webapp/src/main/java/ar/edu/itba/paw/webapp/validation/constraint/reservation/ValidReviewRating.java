package ar.edu.itba.paw.webapp.validation.constraint.reservation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.reservation.ValidReviewRatingValidator;

/** Review rating required (1–5) when submitting a reservation review. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidReviewRatingValidator.class)
public @interface ValidReviewRating {

    String message() default "{validation.review.rating.required}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
