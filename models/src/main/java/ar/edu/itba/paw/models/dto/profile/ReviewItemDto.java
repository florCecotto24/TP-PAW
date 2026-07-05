package ar.edu.itba.paw.models.dto.profile;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Single review row in profile counterparty history (reviewer identity, rating, wall-local date,
 * comment). {@code id}/{@code reservationId} identify the underlying {@code Review} row so REST
 * callers can build its unique canonical URN even though this projection does not hydrate the
 * full entity.
 */
public final class ReviewItemDto {

    private final long id;
    private final long reservationId;
    private final String reviewerName;
    private final int rating;
    private final LocalDate reviewDate;
    private final String comment;

    public ReviewItemDto(
            final long id,
            final long reservationId,
            final String reviewerName,
            final int rating,
            final LocalDate reviewDate,
            final String comment) {
        this.id = id;
        this.reservationId = reservationId;
        this.reviewerName = reviewerName;
        this.rating = rating;
        this.reviewDate = reviewDate;
        this.comment = comment;
    }

    public long getId() {
        return id;
    }

    public long getReservationId() {
        return reservationId;
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
