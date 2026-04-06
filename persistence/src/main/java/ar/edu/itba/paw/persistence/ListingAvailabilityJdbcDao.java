package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ListingAvailability;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ListingAvailabilityJdbcDao implements ListingAvailabilityDao {

    private static final RowMapper<ListingAvailability> ROW_MAPPER = (rs, rowNum) -> new ListingAvailability(
            rs.getLong("id"),
            rs.getLong("listing_id"),
            JdbcDateTimeUtils.readLocalDate(rs, "start_date"),
            JdbcDateTimeUtils.readLocalDate(rs, "end_date"),
            JdbcDateTimeUtils.readOffsetDateTime(rs, "created_at"),
            JdbcDateTimeUtils.readOffsetDateTime(rs, "updated_at"));

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Autowired
    public ListingAvailabilityJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("listing_availability")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public ListingAvailability create(final long listingId, final LocalDate startInclusive,
            final LocalDate endInclusive) {
        final Timestamp now = JdbcDateTimeUtils.nowTimestamp();
        final Map<String, Object> values = new HashMap<>();
        values.put("listing_id", listingId);
        values.put("start_date", JdbcDateTimeUtils.toSqlDate(startInclusive));
        values.put("end_date", JdbcDateTimeUtils.toSqlDate(endInclusive));
        values.put("created_at", now);
        values.put("updated_at", now);
        final Number id = jdbcInsert.executeAndReturnKey(values);
        return new ListingAvailability(
                id.longValue(),
                listingId,
                startInclusive,
                endInclusive,
                JdbcDateTimeUtils.toOffsetDateTime(now),
                JdbcDateTimeUtils.toOffsetDateTime(now));
    }

    @Override
    public List<ListingAvailability> findByListingId(final long listingId) {
        return jdbcTemplate.query(
                "SELECT * FROM listing_availability WHERE listing_id = ? ORDER BY start_date ASC",
                ROW_MAPPER,
                listingId);
    }
}
