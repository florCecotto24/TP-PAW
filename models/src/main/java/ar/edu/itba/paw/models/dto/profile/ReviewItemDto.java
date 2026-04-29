package ar.edu.itba.paw.models.dto.profile;

import java.time.LocalDate;
import java.util.Optional;

public final class ReviewItemDto {

    private final long reviewerUserId;
    private final String reviewerName;
    private final Long reviewerProfileImageId;
    private final int rating;
    private final LocalDate reviewDate;
    private final String comment;

    public ReviewItemDto(
            final long reviewerUserId,
            final String reviewerName,
            final Long reviewerProfileImageId,
            final int rating,
            final LocalDate reviewDate,
            final String comment) {
        this.reviewerUserId = reviewerUserId;
        this.reviewerName = reviewerName;
        this.reviewerProfileImageId = reviewerProfileImageId;
        this.rating = rating;
        this.reviewDate = reviewDate;
        this.comment = comment;
    }

    public long getReviewerUserId() {
        return reviewerUserId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public Optional<Long> getReviewerProfileImageId() {
        return Optional.ofNullable(reviewerProfileImageId);
    }

    public int getRating() {
        return rating;
    }

    public LocalDate getReviewDate() {
        return reviewDate;
    }

    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }
}

