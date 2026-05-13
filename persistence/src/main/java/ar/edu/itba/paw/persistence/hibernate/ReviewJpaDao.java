package ar.edu.itba.paw.persistence.hibernate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.ListingPublicReview;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;
import ar.edu.itba.paw.persistence.ReviewDao;

@Transactional
@Repository
public class ReviewJpaDao implements ReviewDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public boolean existsReview(final long reservationId, final boolean madeByRider) {
        final Number n = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM reviews WHERE reservation_id = :reservationId AND made_by_rider = :madeByRider")
                .setParameter("reservationId", reservationId)
                .setParameter("madeByRider", madeByRider)
                .getSingleResult();
        return n != null && n.longValue() > 0;
    }

    @Override
    public void insertReview(final long reservationId, final boolean madeByRider, final Integer rating, final String comment) {
        em.createNativeQuery(
                        "INSERT INTO reviews (reservation_id, made_by_rider, created_at, rating, comment) VALUES (:reservationId, :madeByRider, :createdAt, :rating, :comment)")
                .setParameter("reservationId", reservationId)
                .setParameter("madeByRider", madeByRider)
                .setParameter("createdAt", new Timestamp(System.currentTimeMillis()))
                .setParameter("rating", rating)
                .setParameter("comment", comment)
                .executeUpdate();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Page<ListingPublicReview> findListingPublicReviews(final long listingId, final int page, final int pageSize) {
        final Number total = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM reviews r "
                                + "INNER JOIN reservations res ON res.id = r.reservation_id "
                                + "WHERE res.listing_id = :listingId AND r.rating IS NOT NULL")
                .setParameter("listingId", listingId)
                .getSingleResult();
        final int offset = page * pageSize;
        final List<Object[]> rows = em.createNativeQuery(
                        "SELECT r.created_at, r.rating, r.comment, "
                                + "CASE WHEN r.made_by_rider THEN ru.forename ELSE ou.forename END AS reviewer_forename, "
                                + "CASE WHEN r.made_by_rider THEN ru.surname ELSE ou.surname END AS reviewer_surname "
                                + "FROM reviews r "
                                + "INNER JOIN reservations res ON res.id = r.reservation_id "
                                + "INNER JOIN listings l ON l.id = res.listing_id "
                                + "INNER JOIN cars c ON c.id = l.car_id "
                                + "INNER JOIN users ru ON ru.id = res.rider_id "
                                + "INNER JOIN users ou ON ou.id = c.owner_id "
                                + "WHERE res.listing_id = :listingId AND r.rating IS NOT NULL "
                                + "ORDER BY r.created_at DESC "
                                + "LIMIT :limit OFFSET :offset")
                .setParameter("listingId", listingId)
                .setParameter("limit", pageSize)
                .setParameter("offset", offset)
                .getResultList();
        final List<ListingPublicReview> content = new ArrayList<>(rows.size());
        for (final Object[] row : rows) {
            content.add(new ListingPublicReview(
                    (String) row[3],
                    (String) row[4],
                    toOffsetDateTime(row[0]),
                    ((Number) row[1]).intValue(),
                    (String) row[2]));
        }
        return new Page<>(content, page, pageSize, total != null ? total.longValue() : 0L);
    }

    @Override
    public void refreshRiderAverageRating(final long riderUserId) {
        final Object raw = em.createNativeQuery(
                        "SELECT ROUND(AVG(r.rating)::numeric, 2) FROM reviews r "
                                + "INNER JOIN reservations res ON res.id = r.reservation_id "
                                + "WHERE r.made_by_rider = FALSE AND res.rider_id = :userId")
                .setParameter("userId", riderUserId)
                .getSingleResult();
        final User user = em.find(User.class, riderUserId);
        if (user == null) {
            return;
        }
        final java.math.BigDecimal avg = toBigDecimalOrNull(raw);
        user.setRatingAsRider(avg);
    }

    @Override
    public void refreshOwnerAverageRating(final long ownerUserId) {
        final Object raw = em.createNativeQuery(
                        "SELECT ROUND(AVG(r.rating)::numeric, 2) FROM reviews r "
                                + "INNER JOIN reservations res ON res.id = r.reservation_id "
                                + "INNER JOIN listings l ON l.id = res.listing_id "
                                + "INNER JOIN cars c ON c.id = l.car_id "
                                + "WHERE r.made_by_rider = TRUE AND c.owner_id = :userId")
                .setParameter("userId", ownerUserId)
                .getSingleResult();
        final User user = em.find(User.class, ownerUserId);
        if (user == null) {
            return;
        }
        user.setRatingAsOwner(toBigDecimalOrNull(raw));
    }

    @Override
    public void refreshListingRatingAvg(final long listingId) {
        final Object raw = em.createNativeQuery(
                        "SELECT ROUND(AVG(r.rating)::numeric, 2) FROM reviews r "
                                + "INNER JOIN reservations res ON res.id = r.reservation_id "
                                + "WHERE res.listing_id = :listingId")
                .setParameter("listingId", listingId)
                .getSingleResult();
        final Listing listing = em.find(Listing.class, listingId);
        if (listing == null) {
            return;
        }
        listing.setRatingAvg(toBigDecimalOrNull(raw));
        listing.setUpdatedAt(OffsetDateTime.now());
    }

    @Override
    public long countReviewsForListing(final long listingId) {
        final Number n = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM reviews r "
                                + "INNER JOIN reservations res ON res.id = r.reservation_id "
                                + "WHERE res.listing_id = :listingId AND r.rating IS NOT NULL")
                .setParameter("listingId", listingId)
                .getSingleResult();
        return n != null ? n.longValue() : 0L;
    }

    @Override
    public BigDecimal findAverageRatingForCounterparty(final long counterpartyUserId, final boolean counterpartyIsOwner) {
        final Object result = em.createNativeQuery(
                        "SELECT ROUND(AVG(r.rating)::numeric, 2) FROM reviews r "
                                + "INNER JOIN reservations res ON res.id = r.reservation_id "
                                + "INNER JOIN listings l ON l.id = res.listing_id "
                                + "INNER JOIN cars c ON c.id = l.car_id "
                                + "WHERE ((:isOwner = TRUE AND r.made_by_rider = TRUE AND c.owner_id = :userId) "
                                + "OR (:isOwner = FALSE AND r.made_by_rider = FALSE AND res.rider_id = :userId))")
                .setParameter("isOwner", counterpartyIsOwner)
                .setParameter("userId", counterpartyUserId)
                .getSingleResult();
        if (result == null) {
            return null;
        }
        if (result instanceof BigDecimal) {
            return (BigDecimal) result;
        }
        return new BigDecimal(result.toString());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<ReviewItemDto> findRecentCommentReviewsForCounterparty(
            final long counterpartyUserId,
            final boolean counterpartyIsOwner,
            final int limit) {
        final List<Object[]> rows = em.createNativeQuery(
                        "SELECT r.rating, r.comment, r.created_at, "
                                + "CASE WHEN r.made_by_rider THEN ru.id ELSE ou.id END AS reviewer_user_id, "
                                + "CASE WHEN r.made_by_rider THEN ru.forename ELSE ou.forename END AS reviewer_forename, "
                                + "CASE WHEN r.made_by_rider THEN ru.surname ELSE ou.surname END AS reviewer_surname, "
                                + "CASE WHEN r.made_by_rider THEN ru.profile_picture_id ELSE ou.profile_picture_id END AS reviewer_profile_picture_id "
                                + "FROM reviews r "
                                + "INNER JOIN reservations res ON res.id = r.reservation_id "
                                + "INNER JOIN listings l ON l.id = res.listing_id "
                                + "INNER JOIN cars c ON c.id = l.car_id "
                                + "INNER JOIN users ru ON ru.id = res.rider_id "
                                + "INNER JOIN users ou ON ou.id = c.owner_id "
                                + "WHERE ((:isOwner = TRUE AND r.made_by_rider = TRUE AND c.owner_id = :userId) "
                                + "OR (:isOwner = FALSE AND r.made_by_rider = FALSE AND res.rider_id = :userId)) "
                                + "AND r.rating IS NOT NULL "
                                + "AND NULLIF(TRIM(r.comment), '') IS NOT NULL "
                                + "ORDER BY r.created_at DESC "
                                + "LIMIT :limit")
                .setParameter("isOwner", counterpartyIsOwner)
                .setParameter("userId", counterpartyUserId)
                .setParameter("limit", limit)
                .getResultList();
        final List<ReviewItemDto> result = new ArrayList<>(rows.size());
        for (final Object[] row : rows) {
            final Object pictureIdObj = row[6];
            final Long pictureId = pictureIdObj == null ? null : ((Number) pictureIdObj).longValue();
            result.add(new ReviewItemDto(
                    ((Number) row[3]).longValue(),
                    (String) row[4] + " " + (String) row[5],
                    pictureId,
                    ((Number) row[0]).intValue(),
                    toOffsetDateTime(row[2]).toLocalDate(),
                    (String) row[1]));
        }
        return result;
    }

    private static OffsetDateTime toOffsetDateTime(final Object o) {
        if (o instanceof OffsetDateTime) {
            return ((OffsetDateTime) o).withOffsetSameInstant(ZoneOffset.UTC);
        }
        if (o instanceof Timestamp) {
            return ((Timestamp) o).toInstant().atOffset(ZoneOffset.UTC);
        }
        return OffsetDateTime.parse(o.toString());
    }

    private static BigDecimal toBigDecimalOrNull(final Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof BigDecimal) {
            return (BigDecimal) raw;
        }
        return new BigDecimal(raw.toString());
    }
}
