package ar.edu.itba.paw.models.dto.profile;

import java.time.LocalDate;
import java.util.Optional;

/** Single review row in profile counterparty history (reviewer identity, rating, wall-local date, comment). */
public final class ReviewItemDto {

    private final String reviewerName;
    private final int rating;
    private final LocalDate reviewDate;
    private final String comment;

    public ReviewItemDto(
            final String reviewerName,
            final int rating,
            final LocalDate reviewDate,
            final String comment) {
        this.reviewerName = reviewerName;
        this.rating = rating;
        this.reviewDate = reviewDate;
        this.comment = comment;
    }

    public String getReviewerName() {
        return reviewerName;
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

    public String getCommentText() {
        return comment != null ? comment : "";
    }
}

