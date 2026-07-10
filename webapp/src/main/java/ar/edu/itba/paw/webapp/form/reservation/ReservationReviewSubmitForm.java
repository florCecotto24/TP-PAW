package ar.edu.itba.paw.webapp.form.reservation;

import ar.edu.itba.paw.webapp.validation.constraint.reservation.ValidReviewComment;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.ValidReviewImagePayload;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.ValidReviewRating;

/**
 * Multipart review submission ({@code POST /reviews?reservationId=…}).
 */
@ValidReviewRating
@ValidReviewImagePayload
public final class ReservationReviewSubmitForm {

    private Integer rating;
    @ValidReviewComment
    private String comment;
    private byte[] imageBytes;
    private String imageContentType;
    private String imageFileName;

    public Integer getRating() {
        return rating;
    }

    public void setRating(final Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageBytes(final byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public void setImageContentType(final String imageContentType) {
        this.imageContentType = imageContentType;
    }

    public String getImageFileName() {
        return imageFileName;
    }

    public void setImageFileName(final String imageFileName) {
        this.imageFileName = imageFileName;
    }
}
