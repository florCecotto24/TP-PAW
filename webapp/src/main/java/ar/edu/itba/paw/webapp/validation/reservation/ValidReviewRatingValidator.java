package ar.edu.itba.paw.webapp.validation.reservation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.form.reservation.ReservationReviewSubmitForm;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.ValidReviewRating;

public final class ValidReviewRatingValidator
        implements ConstraintValidator<ValidReviewRating, ReservationReviewSubmitForm> {

    @Override
    public boolean isValid(final ReservationReviewSubmitForm form, final ConstraintValidatorContext context) {
        if (form == null) {
            return false;
        }
        final Integer rating = form.getRating();
        return rating != null && rating >= 1 && rating <= 5;
    }
}
