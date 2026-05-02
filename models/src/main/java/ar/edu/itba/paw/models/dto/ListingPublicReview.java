package ar.edu.itba.paw.models.dto;

import java.time.OffsetDateTime;
import java.util.Optional;

/** One public review on a listing detail page (reviewer names, instant, rating, comment). */
public final class ListingPublicReview {

    private final String reviewerForename;
    private final String reviewerSurname;
    private final OffsetDateTime createdAt;
    private final int rating;
    private final String comment;

    public ListingPublicReview(
            final String reviewerForename,
            final String reviewerSurname,
            final OffsetDateTime createdAt,
            final int rating,
            final String comment) {
        this.reviewerForename = reviewerForename;
        this.reviewerSurname = reviewerSurname;
        this.createdAt = createdAt;
        this.rating = rating;
        this.comment = comment;
    }

    public String getReviewerForename() {
        return reviewerForename;
    }

    public String getReviewerSurname() {
        return reviewerSurname;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public int getRating() {
        return rating;
    }

    public Optional<String> getComment() {
        return Optional.ofNullable(comment).filter(s -> !s.isBlank());
    }
}
