package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Reservation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.HashMap;
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
            JdbcDateTimeUtils.readOffsetDateTime(rs, "updated_at")
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
    public Reservation createReservation(
            final long riderId,
            final long listingId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final Reservation.Status status) {
        final Timestamp now = JdbcDateTimeUtils.nowTimestamp();
        final Map<String, Object> values = new HashMap<>();
        values.put("rider_id", riderId);
        values.put("listing_id", listingId);
        values.put("start_date", JdbcDateTimeUtils.toTimestamp(startDate));
        values.put("end_date", JdbcDateTimeUtils.toTimestamp(endDate));
        values.put("status", status.name().toLowerCase());
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
                JdbcDateTimeUtils.toOffsetDateTime(now));
    }

    @Override
    public Optional<Reservation> getReservationById(final long id) {
        return jdbcTemplate.query("SELECT * FROM reservations WHERE id = ?", RESERVATION_ROW_MAPPER, id).stream().findAny();
    }
}
