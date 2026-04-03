package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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

import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingSearchCriteria;

@Repository
public class ListingJdbcDao implements ListingDao {

    private static final RowMapper<Listing> LISTING_ROW_MAPPER = (rs, rowNum) -> new Listing(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getLong("car_id"),
            JdbcDateTimeUtils.readOffsetDateTime(rs, "created_at"),
            JdbcDateTimeUtils.readOffsetDateTime(rs, "updated_at"),
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
        final Timestamp now = JdbcDateTimeUtils.nowTimestamp();
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
                JdbcDateTimeUtils.toOffsetDateTime(now),
                JdbcDateTimeUtils.toOffsetDateTime(now),
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

    @Override
    public List<Listing> searchListings(final ListingSearchCriteria criteria) {
        final StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT l.* FROM listings l INNER JOIN cars c ON l.car_id = c.id WHERE l.status = 'active' ");
        final List<Object> args = new ArrayList<>();

        if (criteria.getQuery() != null) {
            final String q = "%" + escapeLike(criteria.getQuery()) + "%";
            sql.append("AND (c.brand ILIKE ? ESCAPE '\\' OR c.model ILIKE ? ESCAPE '\\' OR l.title ILIKE ? ESCAPE '\\' ")
                    .append("OR l.description ILIKE ? ESCAPE '\\' OR l.start_point ILIKE ? ESCAPE '\\') ");
            for (int i = 0; i < 5; i++) {
                args.add(q);
            }
        }

        final boolean hasTrans = !criteria.getTransmissions().isEmpty();
        final boolean hasPwr = !criteria.getPowertrains().isEmpty();
        if (hasTrans || hasPwr) {
            sql.append("AND (");
            if (hasTrans) {
                appendInClause(sql, "c.transmission", criteria.getTransmissions().size());
                args.addAll(criteria.getTransmissions());
                if (hasPwr) {
                    sql.append(" OR ");
                }
            }
            if (hasPwr) {
                appendInClause(sql, "c.powertrain", criteria.getPowertrains().size());
                args.addAll(criteria.getPowertrains());
            }
            sql.append(") ");
        }

        if (!criteria.getCarTypes().isEmpty()) {
            sql.append("AND ");
            appendInClause(sql, "c.type", criteria.getCarTypes().size());
            args.addAll(criteria.getCarTypes());
            sql.append(" ");
        }

        final List<String> bands = criteria.getPriceBands();
        if (!bands.isEmpty()) {
            final boolean wantFree = bands.contains("FREE");
            final boolean wantPaid = bands.contains("PAID");
            if (wantFree ^ wantPaid) {
                if (wantFree) {
                    sql.append("AND l.day_price = 0 ");
                } else {
                    sql.append("AND l.day_price > 0 ");
                }
            }
        }

        if (criteria.hasAvailabilityRange()) {
            final Instant fromInstant = criteria.getAvailabilityRangeStart();
            final Instant untilExclusive = criteria.getAvailabilityRangeEndExclusive();
            final Timestamp fromStart = Timestamp.from(fromInstant);
            final Timestamp untilTs = Timestamp.from(untilExclusive);

            sql.append("AND EXISTS (")
                    .append("SELECT 1 FROM listing_availability la WHERE la.listing_id = l.id ")
                    .append("AND la.start_date <= ? AND la.end_date >= ?) ");
            args.add(fromStart);
            args.add(untilTs);

            sql.append("AND NOT EXISTS (")
                    .append("SELECT 1 FROM reservations r WHERE r.listing_id = l.id ")
                    .append("AND r.status IN ('accepted', 'started') ")
                    .append("AND r.start_date < ? AND r.end_date > ?) ");
            args.add(untilTs);
            args.add(fromStart);
        }

        sql.append("ORDER BY l.created_at DESC");
        return jdbcTemplate.query(sql.toString(), LISTING_ROW_MAPPER, args.toArray());
    }

    @Override
    public List<Listing> getCheapestListings(int limit) {
        return jdbcTemplate.query("SELECT * FROM listings WHERE status = 'active' ORDER BY day_price ASC LIMIT ?", LISTING_ROW_MAPPER, limit);
    }

    @Override
    public List<Listing> getMostRecentListings(int limit) {
        return jdbcTemplate.query("SELECT * FROM listings WHERE status = 'active' ORDER BY created_at DESC LIMIT ?", LISTING_ROW_MAPPER, limit);
    }

    @Override
    public List<Listing> findSimilarListings(
            final long excludedListingId,
            final Car.Type type,
            final Car.Powertrain powertrain,
            final Car.Transmission transmission,
            final int limit) {
        return jdbcTemplate.query(
                "SELECT l.* FROM listings l "
                        + "INNER JOIN cars c ON l.car_id = c.id "
                        + "WHERE l.status = 'active' "
                        + "AND l.id <> ? "
                        + "AND c.type = ? "
                        + "AND c.powertrain = ? "
                        + "AND c.transmission = ? "
                        + "ORDER BY l.created_at DESC "
                        + "LIMIT ?",
                LISTING_ROW_MAPPER,
                excludedListingId,
                type.name(),
                powertrain.name(),
                transmission.name(),
                limit);
    }

    private static void appendInClause(final StringBuilder sql, final String column, final int n) {
        sql.append(column).append(" IN (");
        sql.append(String.join(",", Collections.nCopies(n, "?")));
        sql.append(")");
    }

    private static String escapeLike(final String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
