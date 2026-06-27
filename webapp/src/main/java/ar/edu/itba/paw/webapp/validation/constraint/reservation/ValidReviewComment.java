package ar.edu.itba.paw.webapp.validation.constraint.reservation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.reservation.ValidReviewCommentValidator;

/** Optional review comment bounded by {@link ar.edu.itba.paw.policy.ReviewValidationPolicy}. */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidReviewCommentValidator.class)
public @interface ValidReviewComment {

    String message() default "{validation.review.comment.tooLong}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
