package ar.edu.itba.paw.persistence.jdbc;

import ar.edu.itba.paw.persistence.ReservationDao;
import ar.edu.itba.paw.persistence.util.JdbcDateTimeUtils;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.ReservationCard;
import ar.edu.itba.paw.models.util.ReservationSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ReservationJdbcDao implements ReservationDao {

    private static final String[] ACTIVE_OVERLAP_STATUSES = {"pending", "accepted", "started"};

    private static Reservation mapReservation(final ResultSet rs, final int rowNum) throws SQLException {
        final long pr = rs.getLong("payment_receipt_file_id");
        final Long paymentReceiptId = rs.wasNull() ? null : pr;
        return Reservation.builder()
                .id(rs.getLong("id"))
                .riderId(rs.getLong("rider_id"))
                .listingId(rs.getLong("listing_id"))
                .startDate(JdbcDateTimeUtils.readOffsetDateTime(rs, "start_date"))
                .endDate(JdbcDateTimeUtils.readOffsetDateTime(rs, "end_date"))
                .status(Reservation.Status.valueOf(rs.getString("status").toUpperCase()))
                .createdAt(JdbcDateTimeUtils.readOffsetDateTime(rs, "created_at"))
                .updatedAt(JdbcDateTimeUtils.readOffsetDateTime(rs, "updated_at"))
                .totalPrice(rs.getBigDecimal("total_price"))
                .paymentReceiptFileId(paymentReceiptId)
                .paymentApproved(rs.getBoolean("payment_approved"))
                .paymentProofDeadlineAt(JdbcDateTimeUtils.readOffsetDateTime(rs, "payment_proof_deadline_at"))
                .carReturned(rs.getBoolean("car_returned"))
                .build();
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
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Value("${app.reservation.payment-proof-reminder-lead-hours:2}")
    private int paymentProofReminderLeadHours;

    @Autowired
    public ReservationJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
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
        values.put("return_reminder_email_sent", Boolean.FALSE);
        values.put("return_checkout_email_sent", Boolean.FALSE);
        values.put("rider_review_invite_email_sent", Boolean.FALSE);
        values.put("pending_paymentproof_email_sent", Boolean.FALSE);
        final Number id = jdbcInsert.executeAndReturnKey(values);

        return Reservation.builder()
                .id(id.longValue())
                .riderId(riderId)
                .listingId(listingId)
                .startDate(startDate)
                .endDate(endDate)
                .status(status)
                .createdAt(JdbcDateTimeUtils.toOffsetDateTime(now))
                .updatedAt(JdbcDateTimeUtils.toOffsetDateTime(now))
                .totalPrice(totalPrice)
                .paymentReceiptFileId(null)
                .paymentApproved(false)
                .paymentProofDeadlineAt(paymentProofDeadlineAt)
                .carReturned(false)
                .build();
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
    public Page<ReservationCard> getRiderReservationCards(final ReservationSearchCriteria criteria) {
        final int page = criteria.getPage();
        final int pageSize = criteria.getPageSize();
        final MapSqlParameterSource countParams = new MapSqlParameterSource("riderId", criteria.getRiderId());
        final StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM reservations r "
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "WHERE r.rider_id = :riderId ");
        appendReservationFilters(countSql, countParams, criteria);
        final Long total = namedParameterJdbcTemplate.queryForObject(countSql.toString(), countParams, Long.class);
        final int offset = page * pageSize;
        final MapSqlParameterSource listParams = new MapSqlParameterSource("riderId", criteria.getRiderId())
                .addValue("limit", pageSize)
                .addValue("offset", offset);
        final StringBuilder listSql = new StringBuilder(
                "SELECT r.id AS reservation_id, r.listing_id, r.start_date, r.end_date, r.status, "
                        + "c.brand, c.model, l.day_price, "
                        + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                        + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id "
                        + "FROM reservations r "
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "WHERE r.rider_id = :riderId ");
        appendReservationFilters(listSql, listParams, criteria);
        listSql.append("ORDER BY ").append(buildReservationOrderBy(criteria.getSortBy(), criteria.getSortDirection()))
               .append(" LIMIT :limit OFFSET :offset");
        final List<ReservationCard> content = namedParameterJdbcTemplate.query(
                listSql.toString(), listParams, RESERVATION_CARD_ROW_MAPPER);
        return new Page<>(content, page, pageSize, total != null ? total : 0L);
    }

    @Override
    public Page<ReservationCard> getOwnerReservationCards(final ReservationSearchCriteria criteria) {
        final int page = criteria.getPage();
        final int pageSize = criteria.getPageSize();
        final MapSqlParameterSource countParams = new MapSqlParameterSource("ownerId", criteria.getOwnerId());
        final StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM reservations r "
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "WHERE c.owner_id = :ownerId ");
        appendReservationFilters(countSql, countParams, criteria);
        final Long total = namedParameterJdbcTemplate.queryForObject(countSql.toString(), countParams, Long.class);
        final int offset = page * pageSize;
        final MapSqlParameterSource listParams = new MapSqlParameterSource("ownerId", criteria.getOwnerId())
                .addValue("limit", pageSize)
                .addValue("offset", offset);
        final StringBuilder listSql = new StringBuilder(
                "SELECT r.id AS reservation_id, r.listing_id, r.start_date, r.end_date, r.status, "
                        + "c.brand, c.model, l.day_price, "
                        + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                        + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id "
                        + "FROM reservations r "
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "WHERE c.owner_id = :ownerId ");
        appendReservationFilters(listSql, listParams, criteria);
        listSql.append("ORDER BY ").append(buildReservationOrderBy(criteria.getSortBy(), criteria.getSortDirection()))
               .append(" LIMIT :limit OFFSET :offset");
        final List<ReservationCard> content = namedParameterJdbcTemplate.query(
                listSql.toString(), listParams, RESERVATION_CARD_ROW_MAPPER);
        return new Page<>(content, page, pageSize, total != null ? total : 0L);
    }

    private static void appendReservationFilters(
            final StringBuilder sql,
            final MapSqlParameterSource params,
            final ReservationSearchCriteria criteria) {
        if (!criteria.getStatusFilters().isEmpty()) {
            sql.append("AND LOWER(r.status) IN (:resStatuses) ");
            params.addValue("resStatuses", criteria.getStatusFilters());
        }
        final String textQuery = criteria.getTextQuery();
        if (textQuery != null) {
            final String q = "%" + escapeLike(textQuery) + "%";
            sql.append("AND (LOWER(c.brand) LIKE LOWER(:resSearch) ESCAPE '\\' "
                    + "OR LOWER(c.model) LIKE LOWER(:resSearch) ESCAPE '\\' "
                    + "OR LOWER(l.title) LIKE LOWER(:resSearch) ESCAPE '\\') ");
            params.addValue("resSearch", q);
        }
        if (!criteria.getCarTypes().isEmpty()) {
            sql.append("AND c.type IN (:resCarTypes) ");
            params.addValue("resCarTypes", criteria.getCarTypes());
        }
        if (!criteria.getTransmissions().isEmpty()) {
            sql.append("AND c.transmission IN (:resTransmissions) ");
            params.addValue("resTransmissions", criteria.getTransmissions());
        }
        if (!criteria.getPowertrains().isEmpty()) {
            sql.append("AND c.powertrain IN (:resPowertrains) ");
            params.addValue("resPowertrains", criteria.getPowertrains());
        }
        if (criteria.getMinPrice() != null) {
            sql.append("AND l.day_price >= :resMinPrice ");
            params.addValue("resMinPrice", criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() != null) {
            sql.append("AND l.day_price <= :resMaxPrice ");
            params.addValue("resMaxPrice", criteria.getMaxPrice());
        }
        final List<String> ratingBands = criteria.getRatingBands();
        if (!ratingBands.isEmpty()) {
            final List<String> conditions = new ArrayList<>();
            if (ratingBands.contains("UNDER_2")) {
                conditions.add("l.rating_avg < 2");
            }
            if (ratingBands.contains("2_TO_3")) {
                conditions.add("(l.rating_avg >= 2 AND l.rating_avg < 3)");
            }
            if (ratingBands.contains("3_TO_4")) {
                conditions.add("(l.rating_avg >= 3 AND l.rating_avg < 4)");
            }
            if (ratingBands.contains("OVER_4")) {
                conditions.add("l.rating_avg >= 4");
            }
            if (!conditions.isEmpty()) {
                sql.append("AND (").append(String.join(" OR ", conditions)).append(") ");
            }
        }
    }

    private static String escapeLike(final String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String buildReservationOrderBy(final String sortBy, final String sortDirection) {
        final String dir = "asc".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        if ("price".equals(sortBy)) {
            return "l.day_price " + dir;
        } else if ("rating".equals(sortBy)) {
            return "l.rating_avg " + dir + " NULLS LAST, r.created_at DESC";
        } else {
            return "r.created_at " + dir;
        }
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

    @Override
    public Page<ReservationCard> getListingReservationCards(
            final long ownerId,
            final long listingId,
            final int page,
            final int pageSize,
            final String statusFilter) {
        final List<Object> countArgs = new ArrayList<>();
        countArgs.add(ownerId);
        countArgs.add(listingId);
        final StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM reservations r "
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "WHERE c.owner_id = ? AND r.listing_id = ? ");
        if (statusFilter != null) {
            countSql.append("AND LOWER(r.status) = ? ");
            countArgs.add(statusFilter);
        }
        final Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, countArgs.toArray());
        final int offset = page * pageSize;
        final List<Object> listArgs = new ArrayList<>();
        listArgs.add(ownerId);
        listArgs.add(listingId);
        final StringBuilder listSql = new StringBuilder(
                "SELECT r.id AS reservation_id, r.listing_id, r.start_date, r.end_date, r.status, "
                        + "c.brand, c.model, l.day_price, "
                        + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                        + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id "
                        + "FROM reservations r "
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "WHERE c.owner_id = ? AND r.listing_id = ? ");
        if (statusFilter != null) {
            listSql.append("AND LOWER(r.status) = ? ");
            listArgs.add(statusFilter);
        }
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
    public BigDecimal sumListingRevenueByStatuses(
            final long ownerId,
            final long listingId,
            final Collection<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return BigDecimal.ZERO;
        }
        final List<String> statusList = new ArrayList<>(statuses);
        final String placeholders = String.join(",", Collections.nCopies(statusList.size(), "?"));
        final List<Object> args = new ArrayList<>();
        args.add(ownerId);
        args.add(listingId);
        statusList.forEach(args::add);
        final BigDecimal result = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(r.total_price), 0) FROM reservations r "
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "WHERE c.owner_id = ? AND r.listing_id = ? "
                        + "AND LOWER(r.status) IN (" + placeholders + ")",
                BigDecimal.class,
                args.toArray());
        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public long countListingReservationsCreatedBetween(
            final long ownerId,
            final long listingId,
            final OffsetDateTime from,
            final OffsetDateTime until) {
        final Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservations r "
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "WHERE c.owner_id = ? AND r.listing_id = ? "
                        + "AND r.created_at >= ? AND r.created_at < ?",
                Long.class,
                ownerId,
                listingId,
                JdbcDateTimeUtils.toTimestamp(from),
                JdbcDateTimeUtils.toTimestamp(until));
        return count != null ? count : 0L;
    }

    @Override
    public Optional<OffsetDateTime> findListingNextActiveReservationDate(
            final long ownerId,
            final long listingId,
            final OffsetDateTime after) {
        return jdbcTemplate.query(
                "SELECT r.start_date FROM reservations r "
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "WHERE c.owner_id = ? AND r.listing_id = ? "
                        + "AND LOWER(r.status) IN ('accepted', 'started') "
                        + "AND r.start_date > ? "
                        + "ORDER BY r.start_date ASC LIMIT 1",
                (rs, rn) -> JdbcDateTimeUtils.readOffsetDateTime(rs, "start_date"),
                ownerId,
                listingId,
                JdbcDateTimeUtils.toTimestamp(after))
                .stream().findFirst();
    }

    @Override
    public List<Reservation> findListingFinishedReservations(final long ownerId, final long listingId) {
        return jdbcTemplate.query(
                "SELECT r.* FROM reservations r "
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "WHERE c.owner_id = ? AND r.listing_id = ? "
                        + "AND LOWER(r.status) = 'finished'",
                RESERVATION_ROW_MAPPER,
                ownerId,
                listingId);
    }

    @Override
    public Map<String, Long> countListingReservationsByStatus(final long ownerId, final long listingId) {
        final List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT LOWER(r.status) AS status, COUNT(*) AS cnt "
                        + "FROM reservations r "
                        + "JOIN listings l ON l.id = r.listing_id "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "WHERE c.owner_id = ? AND r.listing_id = ? "
                        + "GROUP BY LOWER(r.status)",
                ownerId, listingId);
        final Map<String, Long> result = new LinkedHashMap<>();
        for (final Map<String, Object> row : rows) {
            result.put((String) row.get("status"), ((Number) row.get("cnt")).longValue());
        }
        return result;
    }

    @Override
    public int markCarReturned(final long reservationId, final long ownerUserId) {
        return jdbcTemplate.update(
                "UPDATE reservations SET car_returned = TRUE, updated_at = ? WHERE id = ? AND listing_id IN ("
                        + "SELECT l.id FROM listings l JOIN cars c ON c.id = l.car_id WHERE c.owner_id = ?)",
                JdbcDateTimeUtils.nowTimestamp(),
                reservationId,
                ownerUserId);
    }

    @Override
    public List<Reservation> findReservationsForReturnReminderEmail(final OffsetDateTime now, final int hoursBeforeCheckout) {
        final OffsetDateTime windowEnd = now.plusHours(hoursBeforeCheckout);
        return jdbcTemplate.query(
                "SELECT * FROM reservations r "
                        + "WHERE LOWER(r.status) IN ('accepted','started') "
                        + "AND r.return_reminder_email_sent = FALSE "
                        + "AND r.end_date > ? "
                        + "AND r.end_date <= ?",
                RESERVATION_ROW_MAPPER,
                JdbcDateTimeUtils.toTimestamp(now),
                JdbcDateTimeUtils.toTimestamp(windowEnd));
    }

    @Override
    public List<Reservation> findReservationsForReturnCheckoutEmail(final OffsetDateTime now) {
        return jdbcTemplate.query(
                "SELECT * FROM reservations r "
                        + "WHERE LOWER(r.status) IN ('accepted','started') "
                        + "AND r.return_checkout_email_sent = FALSE "
                        + "AND r.car_returned = FALSE "
                        + "AND r.end_date <= ?",
                RESERVATION_ROW_MAPPER,
                JdbcDateTimeUtils.toTimestamp(now));
    }

    @Override
    public List<Reservation> findReservationsForRiderReviewInviteEmail(final OffsetDateTime now) {
        return jdbcTemplate.query(
                "SELECT r.* FROM reservations r "
                        + "WHERE LOWER(r.status) IN ('accepted','started','finished') "
                        + "AND r.rider_review_invite_email_sent = FALSE "
                        + "AND r.end_date < ? "
                        + "AND NOT EXISTS (SELECT 1 FROM reviews rv WHERE rv.reservation_id = r.id AND rv.made_by_rider = TRUE)",
                RESERVATION_ROW_MAPPER,
                JdbcDateTimeUtils.toTimestamp(now));
    }

    @Override
    public int claimReturnReminderEmailSent(final long reservationId) {
        return jdbcTemplate.update(
                "UPDATE reservations SET return_reminder_email_sent = TRUE, updated_at = ? "
                        + "WHERE id = ? AND return_reminder_email_sent = FALSE",
                JdbcDateTimeUtils.nowTimestamp(),
                reservationId);
    }

    @Override
    public int claimReturnCheckoutEmailSent(final long reservationId) {
        return jdbcTemplate.update(
                "UPDATE reservations SET return_checkout_email_sent = TRUE, updated_at = ? "
                        + "WHERE id = ? AND return_checkout_email_sent = FALSE",
                JdbcDateTimeUtils.nowTimestamp(),
                reservationId);
    }

    @Override
    public int claimRiderReviewInviteEmailSent(final long reservationId) {
        return jdbcTemplate.update(
                "UPDATE reservations SET rider_review_invite_email_sent = TRUE, updated_at = ? "
                        + "WHERE id = ? AND rider_review_invite_email_sent = FALSE",
                JdbcDateTimeUtils.nowTimestamp(),
                reservationId);
    }

    @Override
    public List<Reservation> findReservationsWithDuePendingPaymentProof(final OffsetDateTime now) {
        final int leadHours = Math.max(1, paymentProofReminderLeadHours);
        final OffsetDateTime windowEnd = now.plusHours(leadHours);
        return jdbcTemplate.query(
                "SELECT * FROM reservations r "
                        + "WHERE r.payment_proof_deadline_at IS NOT NULL "
                        + "AND r.payment_proof_deadline_at <= ? "
                        + "AND r.payment_approved = FALSE "
                        + "AND r.pending_paymentproof_email_sent = FALSE",
                RESERVATION_ROW_MAPPER,
                JdbcDateTimeUtils.toTimestamp(windowEnd));
    }

    @Override
    public int claimPendingPaymentProofEmailSent(final long reservationId) {
        return jdbcTemplate.update(
                "UPDATE reservations SET pending_paymentproof_email_sent = TRUE, updated_at = ? "
                        + "WHERE id = ? AND pending_paymentproof_email_sent = FALSE",
                JdbcDateTimeUtils.nowTimestamp(),
                reservationId);
    }
}
