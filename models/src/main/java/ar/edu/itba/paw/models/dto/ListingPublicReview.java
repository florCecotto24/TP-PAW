package ar.edu.itba.paw.models.dto;

import java.time.OffsetDateTime;
import java.util.Optional;

/** One public review on a listing detail page (reviewer names, instant, rating, comment, optional attached image). */
public final class ListingPublicReview {

    private final String reviewerForename;
    private final String reviewerSurname;
    private final OffsetDateTime createdAt;
    private final int rating;
    private final String comment;
    private final Long imageId;

    public ListingPublicReview(
            final String reviewerForename,
            final String reviewerSurname,
            final OffsetDateTime createdAt,
            final int rating,
            final String comment,
            final Long imageId) {
        this.reviewerForename = reviewerForename;
        this.reviewerSurname = reviewerSurname;
        this.createdAt = createdAt;
        this.rating = rating;
        this.comment = comment;
        this.imageId = imageId;
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

    public Optional<Long> getImageId() {
        return Optional.ofNullable(imageId);
    }
}
