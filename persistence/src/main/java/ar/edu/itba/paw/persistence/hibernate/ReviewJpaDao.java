package ar.edu.itba.paw.persistence.hibernate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.Review;
import ar.edu.itba.paw.models.domain.ReviewId;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.ListingPublicReview;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;
import ar.edu.itba.paw.persistence.ReviewDao;

@Transactional(readOnly = true)
@Repository
public class ReviewJpaDao implements ReviewDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public boolean existsReview(final long reservationId, final boolean madeByRider) {
        return em.find(Review.class, new ReviewId(reservationId, madeByRider)) != null;
    }

    @Override
    @Transactional
    public void insertReview(final long reservationId, final boolean madeByRider, final Integer rating, final String comment) {
        final Reservation reservation = em.find(Reservation.class, reservationId);
        if (reservation == null) {
            return;
        }
        em.persist(new Review(reservation, madeByRider, OffsetDateTime.now(), rating, comment));
    }

    @Override
    @Transactional
    public void refreshRiderAverageRating(final long riderUserId) {
        final Double avg = em.createQuery(
                        "SELECT AVG(r.rating) FROM Review r "
                                + "WHERE r.id.madeByRider = false AND r.reservation.rider.id = :userId "
                                + "AND r.rating IS NOT NULL",
                        Double.class)
                .setParameter("userId", riderUserId)
                .getSingleResult();
        final User user = em.find(User.class, riderUserId);
        if (user != null) {
            user.setRatingAsRider(round2(avg));
        }
    }

    @Override
    @Transactional
    public void refreshOwnerAverageRating(final long ownerUserId) {
        final Double avg = em.createQuery(
                        "SELECT AVG(r.rating) FROM Review r "
                                + "WHERE r.id.madeByRider = true AND r.reservation.car.owner.id = :userId "
                                + "AND r.rating IS NOT NULL",
                        Double.class)
                .setParameter("userId", ownerUserId)
                .getSingleResult();
        final User user = em.find(User.class, ownerUserId);
        if (user != null) {
            user.setRatingAsOwner(round2(avg));
        }
    }

    @Override
    @Transactional
    public void refreshCarRatingAvg(final long carId) {
        final Double avg = em.createQuery(
                        "SELECT AVG(r.rating) FROM Review r "
                                + "WHERE r.reservation.car.id = :carId AND r.rating IS NOT NULL",
                        Double.class)
                .setParameter("carId", carId)
                .getSingleResult();
        final ar.edu.itba.paw.models.domain.Car car = em.find(ar.edu.itba.paw.models.domain.Car.class, carId);
        if (car != null) {
            car.setRatingAvg(round2(avg));
            car.setUpdatedAt(OffsetDateTime.now());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<ListingPublicReview> findCarPublicReviews(final long carId, final int page, final int pageSize) {
        final Long total = em.createQuery(
                        "SELECT COUNT(r) FROM Review r "
                                + "WHERE r.reservation.car.id = :carId AND r.rating IS NOT NULL",
                        Long.class)
                .setParameter("carId", carId)
                .getSingleResult();

        final List<Object[]> pkRows = em.createNativeQuery(
                        "SELECT r.reservation_id, r.made_by_rider "
                                + "FROM reviews r "
                                + "JOIN reservations res ON res.id = r.reservation_id "
                                + "WHERE res.car_id = :carId AND r.rating IS NOT NULL "
                                + "ORDER BY r.created_at DESC "
                                + "LIMIT :limit OFFSET :offset")
                .setParameter("carId", carId)
                .setParameter("limit", pageSize)
                .setParameter("offset", page * pageSize)
                .getResultList();

        if (pkRows.isEmpty()) {
            return new Page<>(Collections.emptyList(), page, pageSize, total != null ? total : 0L);
        }

        final List<ReviewId> ids = pkRows.stream()
                .map(row -> new ReviewId(((Number) row[0]).longValue(), Boolean.TRUE.equals(row[1])))
                .collect(Collectors.toList());
        final List<Review> reviews = em.createQuery(
                        "SELECT r FROM Review r "
                                + "JOIN FETCH r.reservation res "
                                + "JOIN FETCH res.rider rider "
                                + "JOIN FETCH res.car c "
                                + "JOIN FETCH c.owner owner "
                                + "WHERE r.id IN :ids "
                                + "ORDER BY r.createdAt DESC",
                        Review.class)
                .setParameter("ids", ids)
                .getResultList();

        final List<ListingPublicReview> content = reviews.stream()
                .map(r -> {
                    final User reviewer = r.getId().isMadeByRider()
                            ? r.getReservation().getRider()
                            : r.getReservation().getCar().getOwner();
                    return new ListingPublicReview(
                            reviewer.getForename(),
                            reviewer.getSurname(),
                            r.getCreatedAt(),
                            r.getRating().orElse(0),
                            r.getComment().orElse(null));
                })
                .collect(Collectors.toList());
        return new Page<>(content, page, pageSize, total != null ? total : 0L);
    }

    @Override
    public long countReviewsForCar(final long carId) {
        return em.createQuery(
                        "SELECT COUNT(r) FROM Review r "
                                + "WHERE r.reservation.car.id = :carId AND r.rating IS NOT NULL",
                        Long.class)
                .setParameter("carId", carId)
                .getSingleResult();
    }

    @Override
    public BigDecimal findAverageRatingForCounterparty(final long counterpartyUserId, final boolean counterpartyIsOwner) {
        final String jpql = counterpartyIsOwner
                ? "SELECT AVG(r.rating) FROM Review r "
                        + "WHERE r.id.madeByRider = true AND r.reservation.car.owner.id = :userId "
                        + "AND r.rating IS NOT NULL"
                : "SELECT AVG(r.rating) FROM Review r "
                        + "WHERE r.id.madeByRider = false AND r.reservation.rider.id = :userId "
                        + "AND r.rating IS NOT NULL";
        final Double avg = em.createQuery(jpql, Double.class)
                .setParameter("userId", counterpartyUserId)
                .getSingleResult();
        return round2(avg);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<ReviewItemDto> findRecentCommentReviewsForCounterparty(
            final long counterpartyUserId,
            final boolean counterpartyIsOwner,
            final int limit) {
        /*
         * Two native SQL branches to avoid CASE WHEN across different association paths.
         * In each branch the reviewer is statically known (rider vs owner), so the query
         * projects the right columns directly. LIMIT is applied in SQL, consistent with
         * the rest of the codebase.
         */
        final String sql;
        if (counterpartyIsOwner) {
            // counterparty is the owner → reviews written by riders → reviewer = rider
            sql = "SELECT rider.id, rider.forename || ' ' || rider.surname, rider.profile_picture_id, "
                    + "r.rating, r.created_at, r.comment "
                    + "FROM reviews r "
                    + "JOIN reservations res ON res.id = r.reservation_id "
                    + "JOIN users rider ON rider.id = res.rider_id "
                    + "JOIN cars c ON c.id = res.car_id "
                    + "WHERE r.made_by_rider = TRUE AND c.owner_id = :userId "
                    + "AND r.rating IS NOT NULL AND NULLIF(TRIM(r.comment), '') IS NOT NULL "
                    + "ORDER BY r.created_at DESC "
                    + "LIMIT :limit";
        } else {
            // counterparty is the rider → reviews written by owners → reviewer = owner
            sql = "SELECT owner.id, owner.forename || ' ' || owner.surname, owner.profile_picture_id, "
                    + "r.rating, r.created_at, r.comment "
                    + "FROM reviews r "
                    + "JOIN reservations res ON res.id = r.reservation_id "
                    + "JOIN cars c ON c.id = res.car_id "
                    + "JOIN users owner ON owner.id = c.owner_id "
                    + "WHERE r.made_by_rider = FALSE AND res.rider_id = :userId "
                    + "AND r.rating IS NOT NULL AND NULLIF(TRIM(r.comment), '') IS NOT NULL "
                    + "ORDER BY r.created_at DESC "
                    + "LIMIT :limit";
        }
        final List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("userId", counterpartyUserId)
                .setParameter("limit", limit)
                .getResultList();

        final List<ReviewItemDto> result = new ArrayList<>(rows.size());
        for (final Object[] row : rows) {
            final Long pictureId = row[2] == null ? null : ((Number) row[2]).longValue();
            result.add(new ReviewItemDto(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    pictureId,
                    ((Number) row[3]).intValue(),
                    toOffsetDateTime(row[4]).toLocalDate(),
                    (String) row[5]));
        }
        return result;
    }

    private static BigDecimal round2(final Double avg) {
        return avg == null ? null : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
    }

    private static OffsetDateTime toOffsetDateTime(final Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof OffsetDateTime) {
            return ((OffsetDateTime) o).withOffsetSameInstant(ZoneOffset.UTC);
        }
        if (o instanceof Timestamp) {
            return ((Timestamp) o).toInstant().atOffset(ZoneOffset.UTC);
        }
        return OffsetDateTime.parse(o.toString());
    }
}
