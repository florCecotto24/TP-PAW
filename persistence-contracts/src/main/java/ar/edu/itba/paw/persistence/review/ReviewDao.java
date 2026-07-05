package ar.edu.itba.paw.persistence.review;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.domain.review.Review;
import ar.edu.itba.paw.models.dto.car.CarPublicReview;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;

/** Reviews per reservation and aggregated rating maintenance. */
public interface ReviewDao {

    boolean existsReview(long reservationId, boolean madeByRider);

    /** Hydrated single review by its own surrogate id (canonical {@code /reservations/{id}/reviews/{reviewId}}). */
    Optional<Review> findById(long reviewId);

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

    /** Reviews for one reservation (0–2 rows). */
    List<Review> findReviewsForReservation(long reservationId);

    /** Public reviews for a car, paginated. */
    Page<CarPublicReview> findCarPublicReviews(long carId, int page, int pageSize);

    /** Same rows as {@link #findCarPublicReviews} but returns hydrated {@link Review} entities. */
    Page<Review> findPublicReviewsForCar(long carId, int page, int pageSize);

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

    /**
     * Recent rated reviews left to {@code counterpartyUserId} as owner ({@code counterpartyIsOwner=true})
     * or as rider ({@code false}). Comment-less reviews are included; the comment field on
     * {@link ReviewItemDto} is left {@code null} for those rows.
     */
    List<ReviewItemDto> findRecentReviewsForCounterparty(long counterpartyUserId, boolean counterpartyIsOwner, int limit);

    /**
     * Count of rated reviews left to {@code counterpartyUserId} as owner ({@code counterpartyIsOwner=true})
     * or as rider ({@code false}). Mirrors the predicate used by
     * {@link #findAverageRatingForCounterparty(long, boolean)} so the counterparty profile can show
     * "rating (count)" the same way the public car page does.
     */
    long countReviewsForCounterparty(long counterpartyUserId, boolean counterpartyIsOwner);

    /**
     * SQL-paginated feed of every rated review left to {@code userId}, merging both directions
     * (as owner and as rider) in a single {@code LIMIT}/{@code OFFSET} query ordered by date desc —
     * unlike {@link #findRecentReviewsForCounterparty}, which only supports an unpaginated "recent
     * N" preview per direction. Backs {@code GET /users/{id}/reviews}.
     */
    Page<ReviewItemDto> findReviewsForUserPage(long userId, int page, int pageSize);
}
