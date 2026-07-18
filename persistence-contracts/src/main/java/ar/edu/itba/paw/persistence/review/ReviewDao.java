package ar.edu.itba.paw.persistence.review;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.domain.review.Review;

/** Reviews per reservation and aggregated rating maintenance. */
public interface ReviewDao {

    boolean existsReview(long reservationId, boolean madeByRider);

    /** Hydrated single review by its own surrogate id (canonical {@code /reviews/{id}}). */
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

    /** Rated rider→car reviews for a listing, paginated (excludes owner→rider), as hydrated {@link Review} entities. */
    Page<Review> findPublicReviewsForCar(long carId, int page, int pageSize);

    /**
     * SQL-paginated rated reviews received by {@code userId} (both directions), hydrated entities.
     */
    Page<Review> findReviewsReceivedByUser(long userId, int page, int pageSize);

    /**
     * Average rating left to {@code counterpartyUserId} as owner ({@code counterpartyIsOwner=true})
     * or as rider ({@code false}). Returns {@code null} when no rated reviews exist; otherwise a
     * value rounded to two decimals.
     */
    BigDecimal findAverageRatingForCounterparty(long counterpartyUserId, boolean counterpartyIsOwner);

    /**
     * Batch variant of {@link #findAverageRatingForCounterparty(long, boolean)} with
     * {@code counterpartyIsOwner=true}. Missing keys mean no rated reviews for that user.
     */
    Map<Long, BigDecimal> findAverageRatingsAsOwnerForUserIds(Collection<Long> userIds);

    /**
     * Batch variant of {@link #findAverageRatingForCounterparty(long, boolean)} with
     * {@code counterpartyIsOwner=false}. Missing keys mean no rated reviews for that user.
     */
    Map<Long, BigDecimal> findAverageRatingsAsRiderForUserIds(Collection<Long> userIds);

    /**
     * Average rating of rider→car reviews for the listing ({@code madeByRider=true}). Owner→rider
     * reviews are excluded so {@code cars.rating_avg} reflects vehicle quality, not rider scores.
     * Returns {@code null} when no rated rider reviews exist; otherwise a value rounded to two
     * decimals. Persistence of {@code cars.rating_avg} is via
     * {@link CarDao#updateRatingAvg(long, java.math.BigDecimal)}.
     */
    BigDecimal findAverageRatingForCar(long carId);
}
