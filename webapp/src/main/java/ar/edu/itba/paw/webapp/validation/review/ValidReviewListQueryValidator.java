package ar.edu.itba.paw.webapp.validation.review;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.form.review.ReviewListQueryForm;
import ar.edu.itba.paw.webapp.validation.constraint.review.ValidReviewListQuery;

public final class ValidReviewListQueryValidator
        implements ConstraintValidator<ValidReviewListQuery, ReviewListQueryForm> {

    @Override
    public boolean isValid(final ReviewListQueryForm form, final ConstraintValidatorContext context) {
        if (form == null) {
            return true;
        }
        final int filterCount = (form.getCarId() != null ? 1 : 0)
                + (form.getRecipientUserId() != null ? 1 : 0)
                + (form.getReservationId() != null ? 1 : 0);
        return filterCount == 1;
    }
}
