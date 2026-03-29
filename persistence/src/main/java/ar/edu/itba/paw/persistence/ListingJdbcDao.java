package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import ar.edu.itba.paw.models.Listing;

@Repository
public class ListingJdbcDao implements ListingDao {


    private static OffsetDateTime toOffsetDateTime(final Timestamp ts) { // (?) esta funcion la vamos a usar en varios modelos ; quiza conviene ponerla en un archivo aparte
        if (ts == null) {
            return null;
        }
        return ts.toInstant().atOffset(ZoneOffset.UTC);
    }

    private static OffsetDateTime readDateTime(ResultSet rs, String column) throws SQLException {
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

    private static final RowMapper<Listing> LISTING_ROW_MAPPER = (rs, rowNum) -> new Listing(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getLong("car_id"),
            readDateTime(rs, "created_at"),
            readDateTime(rs, "updated_at"),
            Listing.Status.valueOf(rs.getString("status").toUpperCase()),
            rs.getBigDecimal("day_price"),
            rs.getString("start_point"),
            rs.getString("description")
    );

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Autowired
    public ListingJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("listings")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public Listing createListing(
            final long carId,
            final String title,
            final Listing.Status status,
            final BigDecimal dayPrice,
            final String startPoint,
            final String description) {
        final Timestamp now = new Timestamp(System.currentTimeMillis());
        final Map<String, Object> values = new HashMap<>();
        values.put("title", title);
        values.put("car_id", carId);
        values.put("created_at", now);
        values.put("updated_at", now);
        values.put("status", status.name().toLowerCase());
        values.put("day_price", dayPrice);
        values.put("start_point", startPoint);
        values.put("description", description);
        final Number id = jdbcInsert.executeAndReturnKey(values);

        return new Listing(
                id.longValue(),
                title,
                carId,
                toOffsetDateTime(now),
                toOffsetDateTime(now),
                status,
                dayPrice,
                startPoint,
                description);
    }

    @Override
    public Optional<Listing> getListingById(final long id) {
        return jdbcTemplate.query("SELECT * FROM listings WHERE id = ?", LISTING_ROW_MAPPER, id).stream().findAny();
    }

    @Override
    public List<Listing> getAllListings() {
        return jdbcTemplate.query("SELECT * FROM listings ORDER BY created_at DESC", LISTING_ROW_MAPPER);
    }
}
