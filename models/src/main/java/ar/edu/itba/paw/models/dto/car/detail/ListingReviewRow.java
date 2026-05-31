package ar.edu.itba.paw.models.dto.car.detail;

/**
 * Pre-formatted listing review row for the public listing detail carousel and the expanded
 * "all reviews" list. {@code dateText} is already localized; the JSP just renders it.
 *
 * <p>Lives in {@code models} (not {@code webapp}) so the {@code CarDetailViewService} can
 * build a fully-formatted {@code Page} without the controller having to remap rows.</p>
 */
public final class ListingReviewRow {

    private final String reviewerForename;
    private final String reviewerSurname;
    private final String dateText;
    private final int rating;
    private final String comment;
    private final Long imageId;

    public ListingReviewRow(
            final String reviewerForename,
            final String reviewerSurname,
            final String dateText,
            final int rating,
            final String comment,
            final Long imageId) {
        this.reviewerForename = reviewerForename == null ? "" : reviewerForename;
        this.reviewerSurname = reviewerSurname == null ? "" : reviewerSurname;
        this.dateText = dateText == null ? "" : dateText;
        this.rating = rating;
        this.comment = comment == null ? "" : comment;
        this.imageId = imageId;
    }

    public String getReviewerForename() {
        return reviewerForename;
    }

    public String getReviewerSurname() {
        return reviewerSurname;
    }

    public String getDateText() {
        return dateText;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public Long getImageId() {
        return imageId;
    }
}
