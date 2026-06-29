package ar.edu.itba.paw.webapp.validation.reservation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.models.domain.file.Image;
import ar.edu.itba.paw.webapp.form.reservation.ReservationReviewSubmitForm;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.ValidReviewImagePayload;

public final class ValidReviewImagePayloadValidator
        implements ConstraintValidator<ValidReviewImagePayload, ReservationReviewSubmitForm> {

    @Override
    public boolean isValid(final ReservationReviewSubmitForm form, final ConstraintValidatorContext context) {
        if (form == null) {
            return true;
        }
        final byte[] bytes = form.getImageBytes();
        if (bytes == null || bytes.length == 0) {
            return true;
        }
        final String contentType = form.getImageContentType();
        if (contentType == null || !Image.isImageContentType(contentType)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("{profile.picture.notImage}")
                    .addPropertyNode("imageContentType")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
