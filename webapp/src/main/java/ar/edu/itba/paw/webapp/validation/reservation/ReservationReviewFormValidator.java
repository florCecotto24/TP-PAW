package ar.edu.itba.paw.webapp.validation.reservation;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.services.review.ReviewService;
import ar.edu.itba.paw.webapp.form.reservation.ReservationReviewForm;

@Component
public final class ReservationReviewFormValidator implements Validator {

    private final ReviewService reviewService;

    public ReservationReviewFormValidator(final ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Override
    public boolean supports(final Class<?> clazz) {
        return ReservationReviewForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(final Object target, final Errors errors) {
        if (errors.hasErrors()) {
            return;
        }
        final ReservationReviewForm form = (ReservationReviewForm) target;
        final int maxLen = reviewService.getReviewCommentMaxLength();
        final String trimmed = form.getComment() == null ? "" : form.getComment().trim();
        final Integer rating = form.getRating();
        final boolean commentTooLong = trimmed.length() > maxLen;

        if (commentTooLong) {
            errors.rejectValue("comment", MessageKeys.REVIEW_COMMENT_TOO_LONG, new Object[]{maxLen}, null);
        }
        if (rating != null && (rating < 1 || rating > 5)) {
            errors.rejectValue("rating", MessageKeys.REVIEW_RATING_INVALID);
            return;
        }
        if (rating == null && !commentTooLong) {
            if (!trimmed.isEmpty()) {
                errors.rejectValue("rating", MessageKeys.REVIEW_RATING_REQUIRED_WHEN_COMMENT);
            } else {
                errors.rejectValue("rating", "validation.review.rating.required");
            }
        }
    }
}
