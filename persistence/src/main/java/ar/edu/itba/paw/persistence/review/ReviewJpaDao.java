package ar.edu.itba.paw.persistence.review;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.file.Image;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.review.Review;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.persistence.review.ReviewDao;

@Transactional(readOnly = true)
@Repository
public class ReviewJpaDao implements ReviewDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public boolean existsReview(final long reservationId, final boolean madeByRider) {
        final Long count = em.createQuery(
                        "SELECT COUNT(r) FROM Review r "
                                + "WHERE r.reservation.id = :reservationId AND r.madeByRider = :madeByRider",
                        Long.class)
                .setParameter("reservationId", reservationId)
                .setParameter("madeByRider", madeByRider)
                .getSingleResult();
        return count != null && count > 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Review> findById(final long reviewId) {
        return em.createQuery(
                        "SELECT r FROM Review r "
                                + "JOIN FETCH r.reservation res "
                                + "JOIN FETCH res.car c "
                                + "JOIN FETCH c.owner "
                                + "JOIN FETCH res.rider "
                                + "LEFT JOIN FETCH r.image "
                                + "WHERE r.id = :reviewId",
                        Review.class)
                .setParameter("reviewId", reviewId)
                .getResultStream()
                .findFirst();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Review> findReviewsForReservation(final long reservationId) {
        return em.createQuery(
                        "SELECT r FROM Review r "
                                + "JOIN FETCH r.reservation res "
                                + "JOIN FETCH res.car c "
                                + "JOIN FETCH c.owner "
                                + "JOIN FETCH res.rider "
                                + "LEFT JOIN FETCH r.image "
                                + "WHERE res.id = :reservationId",
                        Review.class)
                .setParameter("reservationId", reservationId)
                .getResultList();
    }

    @Override
    @Transactional
    public void insertReview(
            final long reservationId,
            final boolean madeByRider,
            final Integer rating,
            final String comment,
            final Long imageId) {
        // The Reservation is loaded (not getReference) because Review's builder derives the car
        // from it to preserve the (car_id, review) invariant in the entity itself; the car has
        // to be a usable association, not a proxy whose load would happen later. The Image, by
        // contrast, is a pure FK and stays as a proxy.
        final Reservation reservation = em.find(Reservation.class, reservationId);
        if (reservation == null) {
            return;
        }
        final Image imageRef = imageId == null ? null : em.getReference(Image.class, imageId);
        em.persist(Review.builder()
                .reservation(reservation)
                .madeByRider(madeByRider)
                .createdAt(OffsetDateTime.now())
                .rating(rating)
                .comment(comment)
                .image(imageRef)
                .build());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<Review> findPublicReviewsForCar(final long carId, final int page, final int pageSize) {
        final Long total = em.createQuery(
                        "SELECT COUNT(r) FROM Review r "
                                + "WHERE r.reservation.car.id = :carId AND r.madeByRider = true "
                                + "AND r.rating IS NOT NULL",
                        Long.class)
                .setParameter("carId", carId)
                .getSingleResult();

        final List<Number> idRows = em.createNativeQuery(
                        "SELECT r.id "
                                + "FROM reviews r "
                                + "JOIN reservations res ON res.id = r.reservation_id "
                                + "WHERE res.car_id = :carId AND r.made_by_rider = TRUE "
                                + "AND r.rating IS NOT NULL "
                                + "ORDER BY r.created_at DESC, r.id DESC "
                                + "LIMIT :limit OFFSET :offset")
                .setParameter("carId", carId)
                .setParameter("limit", pageSize)
                .setParameter("offset", page * pageSize)
                .getResultList();

        if (idRows.isEmpty()) {
            return new Page<>(Collections.emptyList(), page, pageSize, total != null ? total : 0L);
        }

        final List<Long> ids = idRows.stream().map(Number::longValue).collect(Collectors.toList());
        final List<Review> reviews = hydrateInOrder(ids, em.createQuery(
                        "FROM Review r "
                                + "JOIN FETCH r.reservation res "
                                + "JOIN FETCH res.rider rider "
                                + "JOIN FETCH res.car c "
                                + "JOIN FETCH c.owner owner "
                                + "LEFT JOIN FETCH r.image img "
                                + "WHERE r.id IN :ids",
                        Review.class)
                .setParameter("ids", ids)
                .getResultList());
        return new Page<>(reviews, page, pageSize, total != null ? total : 0L);
    }

    @Override
    public BigDecimal findAverageRatingForCar(final long carId) {
        final Double avg = em.createQuery(
                        "SELECT AVG(r.rating) FROM Review r "
                                + "WHERE r.reservation.car.id = :carId AND r.madeByRider = true "
                                + "AND r.rating IS NOT NULL",
                        Double.class)
                .setParameter("carId", carId)
                .getSingleResult();
        return round2(avg);
    }

    @Override
    public BigDecimal findAverageRatingForCounterparty(final long counterpartyUserId, final boolean counterpartyIsOwner) {
        final String jpql = counterpartyIsOwner
                ? "SELECT AVG(r.rating) FROM Review r "
                        + "WHERE r.madeByRider = true AND r.reservation.car.owner.id = :userId "
                        + "AND r.rating IS NOT NULL"
                : "SELECT AVG(r.rating) FROM Review r "
                        + "WHERE r.madeByRider = false AND r.reservation.rider.id = :userId "
                        + "AND r.rating IS NOT NULL";
        final Double avg = em.createQuery(jpql, Double.class)
                .setParameter("userId", counterpartyUserId)
                .getSingleResult();
        return round2(avg);
    }

    @Override
    public Map<Long, BigDecimal> findAverageRatingsAsOwnerForUserIds(final Collection<Long> userIds) {
        return findAverageRatingsForUserIds(userIds, true);
    }

    @Override
    public Map<Long, BigDecimal> findAverageRatingsAsRiderForUserIds(final Collection<Long> userIds) {
        return findAverageRatingsForUserIds(userIds, false);
    }

    private Map<Long, BigDecimal> findAverageRatingsForUserIds(
            final Collection<Long> userIds, final boolean counterpartyIsOwner) {
        /*
         * Batch scalar aggregate (not entity hydration): one JPQL round-trip with GROUP BY
         * counterparty user id. Path navigation (reservation → car → owner, or reservation → rider)
         * becomes SQL JOINs; mirrors {@link #findAverageRatingForCounterparty(long, boolean)}.
         * Users with no rated reviews are omitted from the map (callers treat missing keys as null).
         * Native SQL would be equivalent here; JPQL keeps the object-graph filter in one place.
         */
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        final String jpql = counterpartyIsOwner
                ? "SELECT r.reservation.car.owner.id, AVG(r.rating) FROM Review r "
                        + "WHERE r.madeByRider = true AND r.reservation.car.owner.id IN :userIds "
                        + "AND r.rating IS NOT NULL GROUP BY r.reservation.car.owner.id"
                : "SELECT r.reservation.rider.id, AVG(r.rating) FROM Review r "
                        + "WHERE r.madeByRider = false AND r.reservation.rider.id IN :userIds "
                        + "AND r.rating IS NOT NULL GROUP BY r.reservation.rider.id";
        final List<Object[]> rows = em.createQuery(jpql, Object[].class)
                .setParameter("userIds", userIds)
                .getResultList();
        final Map<Long, BigDecimal> averages = new java.util.HashMap<>();
        for (final Object[] row : rows) {
            averages.put((Long) row[0], round2((Double) row[1]));
        }
        return averages;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<Review> findReviewsReceivedByUser(final long userId, final int page, final int pageSize) {
        final long total = countReviewsReceivedByUser(userId);
        if (total == 0L) {
            return new Page<>(Collections.emptyList(), page, pageSize, 0L);
        }

        final List<Long> ids = findReviewsReceivedByUserIds(userId, page, pageSize);
        if (ids.isEmpty()) {
            return new Page<>(Collections.emptyList(), page, pageSize, total);
        }
        final List<Review> content = hydrateReviewsReceivedByUserIds(ids);
        return new Page<>(content, page, pageSize, total);
    }

    private long countReviewsReceivedByUser(final long userId) {
        final Long total = em.createQuery(
                        "SELECT COUNT(r) FROM Review r "
                                + "WHERE r.rating IS NOT NULL AND ("
                                + "  (r.madeByRider = true AND r.reservation.car.owner.id = :userId)"
                                + "  OR (r.madeByRider = false AND r.reservation.rider.id = :userId)"
                                + ")",
                        Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
        return total != null ? total : 0L;
    }

    @SuppressWarnings("unchecked")
    private List<Long> findReviewsReceivedByUserIds(
            final long userId, final int page, final int pageSize) {
        final List<Number> idRows = em.createNativeQuery(
                        "SELECT r.id "
                                + "FROM reviews r "
                                + "JOIN reservations res ON res.id = r.reservation_id "
                                + "JOIN cars c ON c.id = res.car_id "
                                + "WHERE r.rating IS NOT NULL AND ("
                                + "  (r.made_by_rider = TRUE AND c.owner_id = :userId)"
                                + "  OR (r.made_by_rider = FALSE AND res.rider_id = :userId)"
                                + ") "
                                + "ORDER BY r.created_at DESC, r.id DESC "
                                + "LIMIT :limit OFFSET :offset")
                .setParameter("userId", userId)
                .setParameter("limit", pageSize)
                .setParameter("offset", page * pageSize)
                .getResultList();
        return idRows.stream().map(Number::longValue).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<Review> hydrateReviewsReceivedByUserIds(final List<Long> ids) {
        final List<Review> hydrated = em.createQuery(
                        "FROM Review r "
                                + "JOIN FETCH r.reservation res "
                                + "JOIN FETCH res.rider rider "
                                + "JOIN FETCH res.car c "
                                + "JOIN FETCH c.owner owner "
                                + "LEFT JOIN FETCH r.image "
                                + "WHERE r.id IN :ids",
                        Review.class)
                .setParameter("ids", ids)
                .getResultList();
        final Map<Long, Review> byId = hydrated.stream()
                .collect(Collectors.toMap(Review::getId, Function.identity()));
        final List<Review> ordered = new ArrayList<>(ids.size());
        for (final Long id : ids) {
            final Review review = byId.get(id);
            if (review != null) {
                ordered.add(review);
            }
        }
        return ordered;
    }

    private static BigDecimal round2(final Double avg) {
        return avg == null ? null : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Reorders the JPQL-hydrated reviews to match the order of the native id step. A JPQL
     * {@code WHERE r.id IN :ids} query does NOT preserve the {@code :ids} order, so without this the
     * page would come back in an arbitrary (implementation-defined) order — breaking the stable,
     * tie-broken {@code created_at DESC, id DESC} ordering established by the id query and causing rows
     * to appear duplicated or missing across pages.
     */
    private static List<Review> hydrateInOrder(final List<Long> ids, final List<Review> hydrated) {
        final Map<Long, Review> byId = hydrated.stream()
                .collect(Collectors.toMap(Review::getId, Function.identity()));
        final List<Review> ordered = new ArrayList<>(ids.size());
        for (final Long id : ids) {
            final Review review = byId.get(id);
            if (review != null) {
                ordered.add(review);
            }
        }
        return ordered;
    }
}
