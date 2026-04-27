package ar.edu.itba.paw.persistence.jdbc;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import ar.edu.itba.paw.models.dto.ListingPublicReview;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.persistence.ReviewDao;
import ar.edu.itba.paw.persistence.util.JdbcDateTimeUtils;

@Repository
public class ReviewJdbcDao implements ReviewDao {

    private static final RowMapper<ListingPublicReview> PUBLIC_REVIEW_MAPPER = (rs, rowNum) -> new ListingPublicReview(
            rs.getString("reviewer_forename"),
            rs.getString("reviewer_surname"),
            JdbcDateTimeUtils.readOffsetDateTime(rs, "created_at"),
            rs.getInt("rating"),
            rs.getString("comment"));

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ReviewJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public boolean existsReview(final long reservationId, final boolean madeByRider) {
        final Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviews WHERE reservation_id = ? AND made_by_rider = ?",
                Integer.class,
                reservationId,
                madeByRider);
        return n != null && n > 0;
    }

    @Override
    public void insertReview(final long reservationId, final boolean madeByRider, final int rating, final String comment) {
        jdbcTemplate.update(
                "INSERT INTO reviews (reservation_id, made_by_rider, created_at, rating, comment) VALUES (?,?,?,?,?)",
                reservationId,
                madeByRider,
                JdbcDateTimeUtils.nowTimestamp(),
                rating,
                comment);
    }

    @Override
    public Page<ListingPublicReview> findListingPublicReviews(final long listingId, final int page, final int pageSize) {
        final Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviews r "
                        + "INNER JOIN reservations res ON res.id = r.reservation_id "
                        + "WHERE res.listing_id = ?",
                Long.class,
                listingId);
        final int offset = page * pageSize;
        final List<Object> args = new ArrayList<>();
        args.add(listingId);
        args.add(pageSize);
        args.add(offset);
        final List<ListingPublicReview> content = jdbcTemplate.query(
                "SELECT r.created_at, r.rating, r.comment, "
                        + "CASE WHEN r.made_by_rider THEN ru.forename ELSE ou.forename END AS reviewer_forename, "
                        + "CASE WHEN r.made_by_rider THEN ru.surname ELSE ou.surname END AS reviewer_surname "
                        + "FROM reviews r "
                        + "INNER JOIN reservations res ON res.id = r.reservation_id "
                        + "INNER JOIN listings l ON l.id = res.listing_id "
                        + "INNER JOIN cars c ON c.id = l.car_id "
                        + "INNER JOIN users ru ON ru.id = res.rider_id "
                        + "INNER JOIN users ou ON ou.id = c.owner_id "
                        + "WHERE res.listing_id = ? "
                        + "ORDER BY r.created_at DESC "
                        + "LIMIT ? OFFSET ?",
                PUBLIC_REVIEW_MAPPER,
                args.toArray());
        return new Page<>(content, page, pageSize, total != null ? total : 0L);
    }

    @Override
    public void refreshRiderAverageRating(final long riderUserId) {
        jdbcTemplate.update(
                "UPDATE users SET rating_as_rider = ("
                        + "SELECT ROUND(AVG(r.rating)::numeric, 2) FROM reviews r "
                        + "INNER JOIN reservations res ON res.id = r.reservation_id "
                        + "WHERE r.made_by_rider = FALSE AND res.rider_id = ?) WHERE id = ?",
                riderUserId,
                riderUserId);
    }

    @Override
    public void refreshOwnerAverageRating(final long ownerUserId) {
        jdbcTemplate.update(
                "UPDATE users SET rating_as_owner = ("
                        + "SELECT ROUND(AVG(r.rating)::numeric, 2) FROM reviews r "
                        + "INNER JOIN reservations res ON res.id = r.reservation_id "
                        + "INNER JOIN listings l ON l.id = res.listing_id "
                        + "INNER JOIN cars c ON c.id = l.car_id "
                        + "WHERE r.made_by_rider = TRUE AND c.owner_id = ?) WHERE id = ?",
                ownerUserId,
                ownerUserId);
    }

    @Override
    public long countReviewsForListing(final long listingId) {
        final Long n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviews r "
                        + "INNER JOIN reservations res ON res.id = r.reservation_id "
                        + "WHERE res.listing_id = ?",
                Long.class,
                listingId);
        return n != null ? n : 0L;
    }

    @Override
    public void refreshListingRatingAvg(final long listingId) {
        jdbcTemplate.update(
                "UPDATE listings SET rating_avg = ("
                        + "SELECT ROUND(AVG(r.rating)::numeric, 2) FROM reviews r "
                        + "INNER JOIN reservations res ON res.id = r.reservation_id "
                        + "WHERE res.listing_id = ?), updated_at = ? WHERE id = ?",
                listingId,
                JdbcDateTimeUtils.nowTimestamp(),
                listingId);
    }
}
