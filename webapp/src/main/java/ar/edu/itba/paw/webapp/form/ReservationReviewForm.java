package ar.edu.itba.paw.webapp.form;

import org.springframework.web.multipart.MultipartFile;

/**
 * Spring form backing object for rider/owner mutual reviews from {@code myReservationDetail}.
 *
 * The optional {@code picture} attaches an image to the review; validation of content type
 * and size happens in {@code MyReservationsController} via {@code MultipartImageValidation},
 * matching the existing convention for profile/car uploads.
 */
public final class ReservationReviewForm {

    private ReservationReviewAction reviewAction = ReservationReviewAction.SUBMIT;
    private Integer rating;
    private String comment;
    private MultipartFile picture;

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

    public MultipartFile getPicture() {
        return picture;
    }

    public void setPicture(final MultipartFile picture) {
        this.picture = picture;
    }
}
