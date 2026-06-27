package ar.edu.itba.paw.webapp.form.reservation;

import ar.edu.itba.paw.webapp.validation.constraint.reservation.ValidReviewComment;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.ValidReviewRating;

/**
 * Multipart review submission ({@code POST /reservations/{id}/reviews}).
 */
@ValidReviewRating
public final class ReservationReviewSubmitForm {

    private Integer rating;
    @ValidReviewComment
    private String comment;

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
}
