package ar.edu.itba.paw.services.support;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.dao.DataIntegrityViolationException;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.review.Review;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.persistence.review.ReviewDao;

/**
 * State-based in-memory fake for {@link ReviewDao} (AGENTS.md rule TEST-8): {@code insertReview}
 * stores a real {@link Review} row and the read-side contract ({@code findReviewsForReservation},
 * {@code findById}, {@code existsReview}, {@code findAverageRating*}, the paginated finders)
 * answers coherently from that stored state. Tests observe writes only through those reads (or
 * through the value the SUT returns after re-reading), never through a recorded call list.
 *
 * <p>{@code stub*} helpers pre-stage read-side state: {@link #stubExistsReview} forces the
 * "already reviewed" answer for negative-path tests, and {@link #stubReservation} registers the
 * {@link Reservation} a subsequent insert will be attached to (the DAO contract only receives the
 * reservation id, but a coherent {@link Review} row needs the entity to resolve its car/rider).
 *
 * <p>Limitations kept deliberately small: the optional {@code imageId} is not materialized into an
 * {@link ar.edu.itba.paw.models.domain.file.Image} (no image rows exist in this fake), and rows
 * whose reservation car has no resolvable owner are excluded from owner-side averages.
 */
public class RecordingReviewDao implements ReviewDao {

    private final Map<String, Boolean> existsReviewByKey = new HashMap<>();
    private final Map<Long, Reservation> reservationsById = new HashMap<>();
    private final Map<Long, Review> reviewsById = new LinkedHashMap<>();
    private long nextId = 1L;

    public void stubExistsReview(final long reservationId, final boolean madeByRider, final boolean value) {
        existsReviewByKey.put(reservationId + ":" + madeByRider, value);
    }

    /** Registers the reservation an {@code insertReview} for its id will be attached to. */
    public void stubReservation(final Reservation reservation) {
        reservationsById.put(reservation.getId(), reservation);
    }

    @Override
    public boolean existsReview(final long reservationId, final boolean madeByRider) {
        final Boolean stubbed = existsReviewByKey.get(reservationId + ":" + madeByRider);
        if (stubbed != null) {
            return stubbed;
        }
        return hasStoredRow(reservationId, madeByRider);
    }

    @Override
    public void insertReview(
            final long reservationId, final boolean madeByRider,
            final Integer rating, final String comment, final Long imageId) {
        if (hasStoredRow(reservationId, madeByRider)) {
            // Mirrors the (reservation_id, made_by_rider) UNIQUE constraint of the real table.
            throw new DataIntegrityViolationException(
                    "duplicate review for reservation " + reservationId + " madeByRider=" + madeByRider);
        }
        final Reservation reservation = reservationsById.get(reservationId);
        if (reservation == null) {
            throw new IllegalStateException(
                    "stubReservation(...) must register reservation " + reservationId + " before insertReview");
        }
        final Review review = Review.builder()
                .reservation(reservation)
                .madeByRider(madeByRider)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .rating(rating)
                .comment(comment)
                .build();
        assignId(review, nextId);
        reviewsById.put(nextId, review);
        nextId++;
    }

    @Override
    public List<Review> findReviewsForReservation(final long reservationId) {
        return reviewsById.values().stream()
                .filter(review -> review.getReservationId() == reservationId)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Review> findById(final long reviewId) {
        return Optional.ofNullable(reviewsById.get(reviewId));
    }

    @Override
    public Page<Review> findPublicReviewsForCar(final long carId, final int page, final int pageSize) {
        final List<Review> rated = reviewsById.values().stream()
                .filter(Review::isMadeByRider)
                .filter(review -> review.getRating().isPresent())
                .filter(review -> carIdOf(review).map(id -> id == carId).orElse(false))
                .collect(Collectors.toList());
        return pageOf(rated, page, pageSize);
    }

    @Override
    public Page<Review> findReviewsReceivedByUser(final long userId, final int page, final int pageSize) {
        final List<Review> received = reviewsById.values().stream()
                .filter(review -> review.getRating().isPresent())
                .filter(review -> review.isMadeByRider()
                        ? ownerIdOf(review).map(id -> id == userId).orElse(false)
                        : review.getReservation().getRiderId() == userId)
                .collect(Collectors.toList());
        return pageOf(received, page, pageSize);
    }

    @Override
    public Optional<BigDecimal> findAverageRatingForCounterparty(
            final long counterpartyUserId, final boolean counterpartyIsOwner) {
        // Rider-made reviews rate the owner; owner-made reviews rate the rider.
        return Optional.ofNullable(average(reviewsById.values().stream()
                .filter(review -> review.isMadeByRider() == counterpartyIsOwner)
                .filter(review -> counterpartyIsOwner
                        ? ownerIdOf(review).map(id -> id == counterpartyUserId).orElse(false)
                        : review.getReservation().getRiderId() == counterpartyUserId)));
    }

    @Override
    public Map<Long, BigDecimal> findAverageRatingsAsOwnerForUserIds(final Collection<Long> userIds) {
        return averagesFor(userIds, true);
    }

    @Override
    public Map<Long, BigDecimal> findAverageRatingsAsRiderForUserIds(final Collection<Long> userIds) {
        return averagesFor(userIds, false);
    }

    @Override
    public Optional<BigDecimal> findAverageRatingForCar(final long carId) {
        return Optional.ofNullable(average(reviewsById.values().stream()
                .filter(Review::isMadeByRider)
                .filter(review -> carIdOf(review).map(id -> id == carId).orElse(false))));
    }

    private boolean hasStoredRow(final long reservationId, final boolean madeByRider) {
        return reviewsById.values().stream()
                .anyMatch(review -> review.getReservationId() == reservationId
                        && review.isMadeByRider() == madeByRider);
    }

    private Map<Long, BigDecimal> averagesFor(final Collection<Long> userIds, final boolean asOwner) {
        final Map<Long, BigDecimal> averages = new HashMap<>();
        for (final Long userId : userIds) {
            findAverageRatingForCounterparty(userId, asOwner)
                    .ifPresent(average -> averages.put(userId, average));
        }
        return averages;
    }

    private static BigDecimal average(final Stream<Review> reviews) {
        final List<Integer> ratings = reviews
                .map(review -> review.getRating().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (ratings.isEmpty()) {
            return null;
        }
        final BigDecimal sum = BigDecimal.valueOf(ratings.stream().mapToInt(Integer::intValue).sum());
        return sum.divide(BigDecimal.valueOf(ratings.size()), 2, RoundingMode.HALF_UP);
    }

    private static Optional<Long> carIdOf(final Review review) {
        final Car car = review.getCar();
        return car == null ? Optional.empty() : Optional.of(car.getId());
    }

    private static Optional<Long> ownerIdOf(final Review review) {
        final Car car = review.getCar();
        if (car == null) {
            return Optional.empty();
        }
        final User owner = car.getOwner();
        return owner == null ? Optional.empty() : Optional.of(owner.getId());
    }

    private static Page<Review> pageOf(final List<Review> all, final int page, final int pageSize) {
        final int fromIndex = Math.min(page * pageSize, all.size());
        final int toIndex = Math.min(fromIndex + pageSize, all.size());
        return new Page<>(all.subList(fromIndex, toIndex), page, pageSize, all.size());
    }

    /** The real column is sequence-assigned by Hibernate; the fake mirrors that outside JPA. */
    private static void assignId(final Review review, final long id) {
        try {
            final Field idField = Review.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.setLong(review, id);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to assign the fake review id", e);
        }
    }
}
