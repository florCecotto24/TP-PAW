package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.models.Page;
import ar.edu.itba.paw.models.ReservationCard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ReservationJdbcDao implements ReservationDao {

    private static final String[] ACTIVE_OVERLAP_STATUSES = {"accepted", "started"};

    private static final RowMapper<Reservation> RESERVATION_ROW_MAPPER = (rs, rowNum) -> new Reservation(
            rs.getLong("id"),
            rs.getLong("rider_id"),
            rs.getLong("listing_id"),
            JdbcDateTimeUtils.readOffsetDateTime(rs, "start_date"),
            JdbcDateTimeUtils.readOffsetDateTime(rs, "end_date"),
            Reservation.Status.valueOf(rs.getString("status").toUpperCase()),
            JdbcDateTimeUtils.readOffsetDateTime(rs, "created_at"),
            JdbcDateTimeUtils.readOffsetDateTime(rs, "updated_at"),
            rs.getBigDecimal("total_price")
    );

    private static final RowMapper<ReservationCard> RESERVATION_CARD_ROW_MAPPER = (rs, rowNum) -> new ReservationCard(
            rs.getLong("reservation_id"),
            rs.getLong("listing_id"),
            rs.getString("brand"),
            rs.getString("model"),
            rs.getBigDecimal("day_price"),
            rs.getLong("image_id"),
            JdbcDateTimeUtils.readOffsetDateTime(rs, "start_date"),
            JdbcDateTimeUtils.readOffsetDateTime(rs, "end_date"),
            Reservation.Status.valueOf(rs.getString("status").toUpperCase())
    );

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Autowired
    public ReservationJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("reservations")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public boolean hasActiveOverlap(
            final long listingId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate) {
        final Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservations " +
                        "WHERE listing_id = ? " +
                        "AND status IN (?, ?) " +
                        "AND start_date < ? " +
                        "AND end_date > ?",
                Integer.class,
                listingId,
                ACTIVE_OVERLAP_STATUSES[0],
                ACTIVE_OVERLAP_STATUSES[1],
                JdbcDateTimeUtils.toTimestamp(endDate),
                JdbcDateTimeUtils.toTimestamp(startDate));
        return count > 0;
    }

    @Override
    public List<Reservation> findBlockingByListingId(final long listingId) {
        return jdbcTemplate.query(
                "SELECT * FROM reservations WHERE listing_id = ? AND LOWER(status) IN (?, ?) ORDER BY start_date ASC",
                RESERVATION_ROW_MAPPER,
                listingId,
                ACTIVE_OVERLAP_STATUSES[0],
                ACTIVE_OVERLAP_STATUSES[1]);
    }

    @Override
    public List<Reservation> findBlockingByListingIds(final Collection<Long> listingIds) {
        if (listingIds == null || listingIds.isEmpty()) {
            return List.of();
        }
        final List<Long> ids = new ArrayList<>(listingIds);
        final String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        final String sql = "SELECT * FROM reservations WHERE listing_id IN (" + placeholders + ") "
                + "AND LOWER(status) IN (?, ?) ORDER BY listing_id ASC, start_date ASC";
        final Object[] args = new Object[ids.size() + 2];
        for (int i = 0; i < ids.size(); i++) {
            args[i] = ids.get(i);
        }
        args[ids.size()] = ACTIVE_OVERLAP_STATUSES[0];
        args[ids.size() + 1] = ACTIVE_OVERLAP_STATUSES[1];
        return jdbcTemplate.query(sql, RESERVATION_ROW_MAPPER, args);
    }

    @Override
    public Reservation createReservation(
            final long riderId,
            final long listingId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final Reservation.Status status,
            final BigDecimal totalPrice) {
        final Timestamp now = JdbcDateTimeUtils.nowTimestamp();
        final Map<String, Object> values = new HashMap<>();
        values.put("rider_id", riderId);
        values.put("listing_id", listingId);
        values.put("start_date", JdbcDateTimeUtils.toTimestamp(startDate));
        values.put("end_date", JdbcDateTimeUtils.toTimestamp(endDate));
        values.put("status", status.name().toLowerCase());
        values.put("total_price", totalPrice);
        values.put("created_at", now);
        values.put("updated_at", now);
        final Number id = jdbcInsert.executeAndReturnKey(values);

        return new Reservation(
                id.longValue(),
                riderId,
                listingId,
                startDate,
                endDate,
                status,
                JdbcDateTimeUtils.toOffsetDateTime(now),
                JdbcDateTimeUtils.toOffsetDateTime(now),
                totalPrice);
    }

    @Override
    public Optional<Reservation> getReservationById(final long id) {
        return jdbcTemplate.query("SELECT * FROM reservations WHERE id = ?", RESERVATION_ROW_MAPPER, id).stream().findAny();
    }

    @Override
    public List<Reservation> getReminderReservations(final OffsetDateTime from, final OffsetDateTime to) {
        return jdbcTemplate.query(
                "SELECT * FROM reservations " +
                        "WHERE status = 'accepted' " +
                        "AND start_date >= ? " +
                        "AND start_date < ?",
                RESERVATION_ROW_MAPPER,
                JdbcDateTimeUtils.toTimestamp(from),
                JdbcDateTimeUtils.toTimestamp(to));
    }

    @Override
    public Optional<Reservation> getOwnerReservationById(final long ownerId, final long reservationId) {
        return jdbcTemplate.query(
                "SELECT r.* FROM reservations r "
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "WHERE r.id = ? AND c.owner_id = ?",
                RESERVATION_ROW_MAPPER,
                reservationId,
                ownerId).stream().findAny();
    }

    @Override
    public Page<ReservationCard> getRiderReservationCards(final long riderId, final int page, final int pageSize) {
        final Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservations WHERE rider_id = ?",
                Long.class,
                riderId);
        final int offset = page * pageSize;
        final List<ReservationCard> content = jdbcTemplate.query(
                "SELECT r.id AS reservation_id, r.listing_id, r.start_date, r.end_date, r.status, "
                        + "c.brand, c.model, l.day_price, "
                        + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                        + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id "
                        + "FROM reservations r "
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "WHERE r.rider_id = ? "
                        + "ORDER BY r.created_at DESC "
                        + "LIMIT ? OFFSET ?",
                RESERVATION_CARD_ROW_MAPPER,
                riderId,
                pageSize,
                offset);
        return new Page<>(content, page, pageSize, total != null ? total : 0L);
    }

    @Override
    public Page<ReservationCard> getOwnerReservationCards(final long ownerId, final int page, final int pageSize) {
        final Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservations r "
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "WHERE c.owner_id = ?",
                Long.class,
                ownerId);
        final int offset = page * pageSize;
        final List<ReservationCard> content = jdbcTemplate.query(
                "SELECT r.id AS reservation_id, r.listing_id, r.start_date, r.end_date, r.status, "
                        + "c.brand, c.model, l.day_price, "
                        + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                        + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id "
                        + "FROM reservations r "
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "WHERE c.owner_id = ? "
                        + "ORDER BY r.created_at DESC "
                        + "LIMIT ? OFFSET ?",
                RESERVATION_CARD_ROW_MAPPER,
                ownerId,
                pageSize,
                offset);
        return new Page<>(content, page, pageSize, total != null ? total : 0L);
    }

    @Override
    public int updateReservationStatus(final long reservationId, final String status) {
        return jdbcTemplate.update(
                "UPDATE reservations SET status = ?, updated_at = ? WHERE id = ?",
                status,
                JdbcDateTimeUtils.nowTimestamp(),
                reservationId);
    }

    @Override
    public List<Reservation> getListingActiveReservations(final long listingId) {
        return jdbcTemplate.query(
                "SELECT * FROM reservations WHERE listing_id = ? AND status in (? , ?) ORDER BY start_date ASC",
                RESERVATION_ROW_MAPPER,
                listingId,
                "accepted",
                "started");
    }
}
