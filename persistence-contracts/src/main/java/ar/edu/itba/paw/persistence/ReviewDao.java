package ar.edu.itba.paw.persistence;

import java.util.List;
import java.math.BigDecimal;

import ar.edu.itba.paw.models.dto.ListingPublicReview;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;

/** Reviews per reservation and aggregated rating maintenance. */
public interface ReviewDao {

    boolean existsReview(long reservationId, boolean madeByRider);

    /** {@code rating} {@code null} persists an omitted review (same PK slot; excluded from public aggregates). */
    void insertReview(long reservationId, boolean madeByRider, Integer rating, String comment);

    /** Public reviews for a car, paginated. */
    Page<ListingPublicReview> findCarPublicReviews(long carId, int page, int pageSize);

    /** Total reviews stored against the car. */
    long countReviewsForCar(long carId);

    void refreshRiderAverageRating(long riderUserId);

    void refreshOwnerAverageRating(long ownerUserId);

    /** Updates {@code cars.rating_avg} from reservations resolved by {@code car_id}. */
    void refreshCarRatingAvg(long carId);

    BigDecimal findAverageRatingForCounterparty(long counterpartyUserId, boolean counterpartyIsOwner);

    List<ReviewItemDto> findRecentCommentReviewsForCounterparty(long counterpartyUserId, boolean counterpartyIsOwner, int limit);
}
