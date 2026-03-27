package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Reservation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class ReservationJdbcDao implements ReservationDao {

    private static OffsetDateTime toOffsetDateTime(final Timestamp ts) {
        if (ts == null) {
            return null;
        }
        return ts.toInstant().atOffset(ZoneOffset.UTC);
    }

    private static OffsetDateTime readDateTime(final ResultSet rs, final String column) throws SQLException {
        final Object o = rs.getObject(column);
        if (o == null) {
            return null;
        }
        if (o instanceof OffsetDateTime) {
            return (OffsetDateTime) o;
        }
        if (o instanceof Timestamp) {
            return toOffsetDateTime((Timestamp) o);
        }
        return OffsetDateTime.parse(o.toString());
    }

    private static final RowMapper<Reservation> RESERVATION_ROW_MAPPER = (rs, rowNum) -> new Reservation(
            rs.getLong("id"),
            rs.getLong("rider_id"),
            rs.getLong("listing_id"),
            readDateTime(rs, "start_date"),
            readDateTime(rs, "end_date"),
            Reservation.Status.valueOf(rs.getString("status").toUpperCase()),
            readDateTime(rs, "created_at"),
            readDateTime(rs, "updated_at")
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
    public Reservation createReservation(
            final long riderId,
            final long listingId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final Reservation.Status status) {
        final Timestamp now = new Timestamp(System.currentTimeMillis());
        final Map<String, Object> values = new HashMap<>();
        values.put("rider_id", riderId);
        values.put("listing_id", listingId);
        values.put("start_date", Timestamp.from(startDate.toInstant()));
        values.put("end_date", Timestamp.from(endDate.toInstant()));
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
                toOffsetDateTime(now),
                toOffsetDateTime(now));
    }

    @Override
    public Optional<Reservation> getReservationById(final long id) {
        return jdbcTemplate.query("SELECT * FROM reservations WHERE id = ?", RESERVATION_ROW_MAPPER, id).stream().findAny();
    }
}
