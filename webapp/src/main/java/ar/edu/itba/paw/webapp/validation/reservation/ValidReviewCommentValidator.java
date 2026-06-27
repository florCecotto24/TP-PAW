package ar.edu.itba.paw.webapp.validation.reservation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.policy.ReviewValidationPolicy;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.ValidReviewComment;

@Component
public final class ValidReviewCommentValidator implements ConstraintValidator<ValidReviewComment, String> {

    private final ReviewValidationPolicy reviewValidationPolicy;

    public ValidReviewCommentValidator(final ReviewValidationPolicy reviewValidationPolicy) {
        this.reviewValidationPolicy = reviewValidationPolicy;
    }

    @Override
    public boolean isValid(final String comment, final ConstraintValidatorContext context) {
        if (comment == null) {
            return true;
        }
        return comment.trim().length() <= reviewValidationPolicy.getCommentMaxLength();
    }
}
