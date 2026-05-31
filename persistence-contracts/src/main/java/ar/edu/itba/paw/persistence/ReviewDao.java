package ar.edu.itba.paw.persistence;

import java.util.List;
import java.math.BigDecimal;

import ar.edu.itba.paw.models.dto.listing.ListingPublicReview;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;

/** Reviews per reservation and aggregated rating maintenance. */
public interface ReviewDao {

    boolean existsReview(long reservationId, boolean madeByRider);

    /** {@code rating} {@code null} persists an omitted review (same PK slot; excluded from public aggregates). */
    default void insertReview(long reservationId, boolean madeByRider, Integer rating, String comment) {
        insertReview(reservationId, madeByRider, rating, comment, null);
    }

    /**
     * Same as {@link #insertReview(long, boolean, Integer, String)} but allows attaching an optional image
     * already persisted in the {@code images} table. Pass {@code null} for {@code imageId} when no image
     * should be attached.
     */
    void insertReview(long reservationId, boolean madeByRider, Integer rating, String comment, Long imageId);

    /** Public reviews for a car, paginated. */
    Page<ListingPublicReview> findCarPublicReviews(long carId, int page, int pageSize);

    /** Total reviews stored against the car. */
    long countReviewsForCar(long carId);

    /**
     * Average rating left to {@code counterpartyUserId} as owner ({@code counterpartyIsOwner=true})
     * or as rider ({@code false}). Returns {@code null} when no rated reviews exist; otherwise a
     * value rounded to two decimals.
     */
    BigDecimal findAverageRatingForCounterparty(long counterpartyUserId, boolean counterpartyIsOwner);

    /**
     * Average rating for a car's reviews (both sides). Returns {@code null} when no rated reviews
     * exist; otherwise a value rounded to two decimals. The actual persistence of {@code cars.rating_avg}
     * is the responsibility of {@link CarDao#updateRatingAvg(long, java.math.BigDecimal)}.
     */
    BigDecimal findAverageRatingForCar(long carId);

    List<ReviewItemDto> findRecentCommentReviewsForCounterparty(long counterpartyUserId, boolean counterpartyIsOwner, int limit);
}
