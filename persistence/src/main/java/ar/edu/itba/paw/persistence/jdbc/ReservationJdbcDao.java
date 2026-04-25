package ar.edu.itba.paw.persistence.jdbc;

import ar.edu.itba.paw.persistence.ReservationDao;
import ar.edu.itba.paw.persistence.util.JdbcDateTimeUtils;
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
import java.sql.ResultSet;
import java.sql.SQLException;
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

    private static final String[] ACTIVE_OVERLAP_STATUSES = {"pending", "accepted", "started"};

    private static Reservation mapReservation(final ResultSet rs, final int rowNum) throws SQLException {
        final long pr = rs.getLong("payment_receipt_file_id");
        final Long paymentReceiptId = rs.wasNull() ? null : pr;
        return new Reservation(
                rs.getLong("id"),
                rs.getLong("rider_id"),
                rs.getLong("listing_id"),
                JdbcDateTimeUtils.readOffsetDateTime(rs, "start_date"),
                JdbcDateTimeUtils.readOffsetDateTime(rs, "end_date"),
                Reservation.Status.valueOf(rs.getString("status").toUpperCase()),
                JdbcDateTimeUtils.readOffsetDateTime(rs, "created_at"),
                JdbcDateTimeUtils.readOffsetDateTime(rs, "updated_at"),
                rs.getBigDecimal("total_price"),
                paymentReceiptId,
                rs.getBoolean("payment_approved"),
                JdbcDateTimeUtils.readOffsetDateTime(rs, "payment_proof_deadline_at"),
                rs.getBoolean("car_returned"));
    }

    private static final RowMapper<Reservation> RESERVATION_ROW_MAPPER = (rs, rowNum) -> {
        try {
            return mapReservation(rs, rowNum);
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    };

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
                        "AND status IN (?, ?, ?) " +
                        "AND start_date < ? " +
                        "AND end_date > ?",
                Integer.class,
                listingId,
                ACTIVE_OVERLAP_STATUSES[0],
                ACTIVE_OVERLAP_STATUSES[1],
                ACTIVE_OVERLAP_STATUSES[2],
                JdbcDateTimeUtils.toTimestamp(endDate),
                JdbcDateTimeUtils.toTimestamp(startDate));
        return count > 0;
    }

    @Override
    public List<Reservation> findBlockingByListingId(final long listingId) {
        return jdbcTemplate.query(
                "SELECT * FROM reservations WHERE listing_id = ? AND LOWER(status) IN (?, ?, ?) ORDER BY start_date ASC",
                RESERVATION_ROW_MAPPER,
                listingId,
                ACTIVE_OVERLAP_STATUSES[0],
                ACTIVE_OVERLAP_STATUSES[1],
                ACTIVE_OVERLAP_STATUSES[2]);
    }

    @Override
    public List<Reservation> findBlockingByListingIds(final Collection<Long> listingIds) {
        if (listingIds == null || listingIds.isEmpty()) {
            return List.of();
        }
        final List<Long> ids = new ArrayList<>(listingIds);
        final String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        final String sql = "SELECT * FROM reservations WHERE listing_id IN (" + placeholders + ") "
                + "AND LOWER(status) IN (?, ?, ?) ORDER BY listing_id ASC, start_date ASC";
        final Object[] args = new Object[ids.size() + 3];
        for (int i = 0; i < ids.size(); i++) {
            args[i] = ids.get(i);
        }
        args[ids.size()] = ACTIVE_OVERLAP_STATUSES[0];
        args[ids.size() + 1] = ACTIVE_OVERLAP_STATUSES[1];
        args[ids.size() + 2] = ACTIVE_OVERLAP_STATUSES[2];
        return jdbcTemplate.query(sql, RESERVATION_ROW_MAPPER, args);
    }

    @Override
    public Reservation createReservation(
            final long riderId,
            final long listingId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final Reservation.Status status,
            final BigDecimal totalPrice,
            final OffsetDateTime paymentProofDeadlineAt) {
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
        values.put("payment_proof_deadline_at", JdbcDateTimeUtils.toTimestamp(paymentProofDeadlineAt));
        values.put("payment_approved", Boolean.FALSE);
        values.put("car_returned", Boolean.FALSE);
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
                totalPrice,
                null,
                false,
                paymentProofDeadlineAt,
                false);
    }

    @Override
    public List<Reservation> findPendingPaymentPastDeadline(final OffsetDateTime now) {
        return jdbcTemplate.query(
                "SELECT * FROM reservations WHERE LOWER(status) = 'pending' "
                        + "AND payment_proof_deadline_at IS NOT NULL AND payment_proof_deadline_at < ? "
                        + "ORDER BY id ASC",
                RESERVATION_ROW_MAPPER,
                JdbcDateTimeUtils.toTimestamp(now));
    }

    @Override
    public int attachPaymentReceiptAndAccept(final long reservationId, final long riderId, final long storedFileId) {
        return jdbcTemplate.update(
                "UPDATE reservations SET status = 'accepted', payment_receipt_file_id = ?, updated_at = ? "
                        + "WHERE id = ? AND rider_id = ? AND LOWER(status) = 'pending'",
                storedFileId,
                JdbcDateTimeUtils.nowTimestamp(),
                reservationId,
                riderId);
    }

    @Override
    public int updatePaymentApproved(final long reservationId, final long ownerUserId, final boolean approved) {
        return jdbcTemplate.update(
                "UPDATE reservations SET payment_approved = ?, updated_at = ? WHERE id = ? AND listing_id IN ("
                        + "SELECT l.id FROM listings l JOIN cars c ON c.id = l.car_id WHERE c.owner_id = ?)",
                approved,
                JdbcDateTimeUtils.nowTimestamp(),
                reservationId,
                ownerUserId);
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
    public Page<ReservationCard> getRiderReservationCards(
            final long riderId,
            final int page,
            final int pageSize,
            final String statusFilter) {
        final List<Object> countArgs = new ArrayList<>();
        countArgs.add(riderId);
        final StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM reservations WHERE rider_id = ? ");
        appendReservationStatusFilter(countSql, countArgs, statusFilter, "status");
        final Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, countArgs.toArray());
        final int offset = page * pageSize;
        final List<Object> listArgs = new ArrayList<>();
        listArgs.add(riderId);
        final StringBuilder listSql = new StringBuilder(
                "SELECT r.id AS reservation_id, r.listing_id, r.start_date, r.end_date, r.status, "
                        + "c.brand, c.model, l.day_price, "
                        + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                        + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id "
                        + "FROM reservations r "
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "WHERE r.rider_id = ? ");
        appendReservationStatusFilter(listSql, listArgs, statusFilter, "r.status");
        listSql.append("ORDER BY r.created_at DESC LIMIT ? OFFSET ?");
        listArgs.add(pageSize);
        listArgs.add(offset);
        final List<ReservationCard> content = jdbcTemplate.query(
                listSql.toString(),
                RESERVATION_CARD_ROW_MAPPER,
                listArgs.toArray());
        return new Page<>(content, page, pageSize, total != null ? total : 0L);
    }

    private static void appendReservationStatusFilter(
            final StringBuilder sql,
            final List<Object> args,
            final String statusFilter,
            final String statusColumnSql) {
        if (statusFilter == null || statusFilter.isBlank()) {
            return;
        }
        final String sf = statusFilter.trim().toLowerCase();
        if ("pending".equals(sf) || "accepted".equals(sf) || "started".equals(sf) || "cancelled".equals(sf)
                || "finished".equals(sf)) {
            sql.append("AND LOWER(").append(statusColumnSql).append(") = ? ");
            args.add(sf);
        }
    }

    @Override
    public Page<ReservationCard> getOwnerReservationCards(
            final long ownerId,
            final int page,
            final int pageSize,
            final String statusFilter) {
        final List<Object> countArgs = new ArrayList<>();
        countArgs.add(ownerId);
        final StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM reservations r "
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "WHERE c.owner_id = ? ");
        appendReservationStatusFilter(countSql, countArgs, statusFilter, "r.status");
        final Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, countArgs.toArray());
        final int offset = page * pageSize;
        final List<Object> listArgs = new ArrayList<>();
        listArgs.add(ownerId);
        final StringBuilder listSql = new StringBuilder(
                "SELECT r.id AS reservation_id, r.listing_id, r.start_date, r.end_date, r.status, "
                        + "c.brand, c.model, l.day_price, "
                        + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                        + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id "
                        + "FROM reservations r "
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "WHERE c.owner_id = ? ");
        appendReservationStatusFilter(listSql, listArgs, statusFilter, "r.status");
        listSql.append("ORDER BY r.created_at DESC LIMIT ? OFFSET ?");
        listArgs.add(pageSize);
        listArgs.add(offset);
        final List<ReservationCard> content = jdbcTemplate.query(
                listSql.toString(),
                RESERVATION_CARD_ROW_MAPPER,
                listArgs.toArray());
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
                        "SELECT * FROM reservations WHERE listing_id = ? AND LOWER(status) IN ('pending', 'accepted', 'started') "
                        + "ORDER BY start_date ASC",
                RESERVATION_ROW_MAPPER,
                listingId);
    }
}
