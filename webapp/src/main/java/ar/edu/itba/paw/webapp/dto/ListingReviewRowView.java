package ar.edu.itba.paw.webapp.dto;

/**
 * Pre-formatted listing review row for the public listing detail carousel.
 */
public final class ListingReviewRowView {

    private final String reviewerForename;
    private final String reviewerSurname;
    private final String dateText;
    private final int rating;
    private final String comment;

    public ListingReviewRowView(
            final String reviewerForename,
            final String reviewerSurname,
            final String dateText,
            final int rating,
            final String comment) {
        this.reviewerForename = reviewerForename == null ? "" : reviewerForename;
        this.reviewerSurname = reviewerSurname == null ? "" : reviewerSurname;
        this.dateText = dateText == null ? "" : dateText;
        this.rating = rating;
        this.comment = comment == null ? "" : comment;
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
}
