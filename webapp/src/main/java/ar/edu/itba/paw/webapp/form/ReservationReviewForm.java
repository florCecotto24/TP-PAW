package ar.edu.itba.paw.webapp.form;

/**
 * Spring form backing object for rider/owner mutual reviews from {@code myReservationDetail}.
 */
public final class ReservationReviewForm {

    private ReservationReviewAction reviewAction = ReservationReviewAction.SUBMIT;
    private Integer rating;
    private String comment;

    public ReservationReviewAction getReviewAction() {
        return reviewAction;
    }

    public void setReviewAction(final ReservationReviewAction reviewAction) {
        this.reviewAction = reviewAction != null ? reviewAction : ReservationReviewAction.SUBMIT;
    }

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
