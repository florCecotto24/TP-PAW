package ar.edu.itba.paw.services.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.review.Review;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.CarPublicReview;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;
import ar.edu.itba.paw.persistence.review.ReviewDao;

/**
 * State-based test double for {@link ReviewDao}. Tests stage return values for the
 * read-side methods (e.g. {@code existsReview}) through the {@code stub*} helpers and
 * assert on the {@code inserted()} list to verify the SUT persisted the right rows.
 * This replaces the {@code Mockito.doAnswer(...).when(reviewDao).insertReview(...)}
 * captor pattern banned by AGENTS.md rule TEST-8.
 */
public class RecordingReviewDao implements ReviewDao {

    public record InsertedReview(
            long reservationId, boolean madeByRider, Integer rating, String comment, Long imageId) { }

    private final Map<String, Boolean> existsReviewByKey = new HashMap<>();
    private final List<InsertedReview> inserted = new ArrayList<>();
    private final Map<String, Page<CarPublicReview>> carPublicReviewsByKey = new HashMap<>();
    private final Map<Long, Long> reviewCountByCarId = new HashMap<>();

    public List<InsertedReview> inserted() { return inserted; }

    public void stubExistsReview(final long reservationId, final boolean madeByRider, final boolean value) {
        existsReviewByKey.put(reservationId + ":" + madeByRider, value);
    }

    public void stubCarPublicReviews(
            final long carId, final int page, final int pageSize, final Page<CarPublicReview> result) {
        carPublicReviewsByKey.put(carId + ":" + page + ":" + pageSize, result);
    }

    public void stubReviewCountForCar(final long carId, final long count) {
        reviewCountByCarId.put(carId, count);
    }

    @Override
    public boolean existsReview(final long reservationId, final boolean madeByRider) {
        return existsReviewByKey.getOrDefault(reservationId + ":" + madeByRider, false);
    }

    @Override
    public void insertReview(
            final long reservationId, final boolean madeByRider,
            final Integer rating, final String comment, final Long imageId) {
        inserted.add(new InsertedReview(reservationId, madeByRider, rating, comment, imageId));
    }

    @Override
    public Page<CarPublicReview> findCarPublicReviews(final long carId, final int page, final int pageSize) {
        return carPublicReviewsByKey.getOrDefault(carId + ":" + page + ":" + pageSize,
                new Page<>(List.of(), page, pageSize, 0L));
    }

    @Override
    public Page<Review> findPublicReviewsForCar(final long carId, final int page, final int pageSize) {
        return new Page<>(List.of(), page, pageSize, 0L);
    }

    @Override
    public List<Review> findReviewsForReservation(final long reservationId) {
        return List.of();
    }

    @Override
    public Optional<Review> findById(final long reviewId) {
        return Optional.empty();
    }

    @Override
    public long countReviewsForCar(final long carId) {
        return reviewCountByCarId.getOrDefault(carId, 0L);
    }

    @Override
    public BigDecimal findAverageRatingForCounterparty(
            final long counterpartyUserId, final boolean counterpartyIsOwner) {
        return null;
    }

    @Override
    public Map<Long, BigDecimal> findAverageRatingsAsOwnerForUserIds(final Collection<Long> userIds) {
        return Map.of();
    }

    @Override
    public Map<Long, BigDecimal> findAverageRatingsAsRiderForUserIds(final Collection<Long> userIds) {
        return Map.of();
    }

    @Override
    public BigDecimal findAverageRatingForCar(final long carId) { return null; }

    @Override
    public List<ReviewItemDto> findRecentReviewsForCounterparty(
            final long counterpartyUserId, final boolean counterpartyIsOwner, final int limit) {
        return List.of();
    }

    @Override
    public long countReviewsForCounterparty(
            final long counterpartyUserId, final boolean counterpartyIsOwner) {
        return 0L;
    }

    @Override
    public Page<Review> findReviewsReceivedByUser(final long userId, final int page, final int pageSize) {
        return new Page<>(List.of(), page, pageSize, 0L);
    }

    @Override
    public Page<ReviewItemDto> findReviewsForUserPage(final long userId, final int page, final int pageSize) {
        return new Page<>(List.of(), page, pageSize, 0L);
    }
}
