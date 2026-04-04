package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.CarPicture;
import ar.edu.itba.paw.models.HomeListingCards;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingAvailability;
import ar.edu.itba.paw.models.ListingCard;
import ar.edu.itba.paw.models.ListingDetail;
import ar.edu.itba.paw.models.ListingSearchCriteria;
import ar.edu.itba.paw.models.User;

@Repository
public class ListingJdbcDao implements ListingDao {

    private static final String HOME_SECTION_CHEAPEST = "C";
    private static final String HOME_SECTION_RECENT = "R";

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

    private static final RowMapper<ListingCard> LISTING_CARD_ROW_MAPPER = (rs, rowNum) -> new ListingCard(
            rs.getLong("listing_id"),
            rs.getString("brand"),
            rs.getString("model"),
            rs.getBigDecimal("day_price"),
            rs.getLong("image_id")
    );

    private static final ResultSetExtractor<Optional<ListingDetail>> LISTING_DETAIL_EXTRACTOR = rs -> {
        Listing listing = null;
        Car car = null;
        User owner = null;
        final LinkedHashMap<Long, CarPicture> pictures = new LinkedHashMap<>();
        final LinkedHashMap<Long, ListingAvailability> availabilities = new LinkedHashMap<>();

        while (rs.next()) {
            if (listing == null) {
                listing = new Listing(
                        rs.getLong("listing_id"),
                        rs.getString("title"),
                        rs.getLong("car_id"),
                        JdbcDateTimeUtils.readOffsetDateTime(rs, "listing_created_at"),
                        JdbcDateTimeUtils.readOffsetDateTime(rs, "listing_updated_at"),
                        Listing.Status.valueOf(rs.getString("status").toUpperCase()),
                        rs.getBigDecimal("day_price"),
                        rs.getString("start_point"),
                        rs.getString("description"));
                car = new Car(
                        rs.getLong("car_id"),
                        rs.getLong("owner_id"),
                        rs.getString("plate"),
                        rs.getString("brand"),
                        rs.getString("model"),
                        Car.Type.valueOf(rs.getString("type")),
                        Car.Powertrain.valueOf(rs.getString("powertrain")),
                        Car.Transmission.valueOf(rs.getString("transmission")));
                owner = new User(
                        rs.getLong("owner_user_id"),
                        rs.getString("owner_email"),
                        rs.getString("owner_forename"),
                        rs.getString("owner_surname"));
            }

            final long cpId = rs.getLong("car_picture_id");
            if (!rs.wasNull() && !pictures.containsKey(cpId)) {
                pictures.put(cpId, new CarPicture(
                        cpId,
                        rs.getLong("car_id"),
                        rs.getLong("image_id"),
                        rs.getInt("display_order"),
                        JdbcDateTimeUtils.toOffsetDateTime(rs.getTimestamp("cp_created_at")),
                        JdbcDateTimeUtils.toOffsetDateTime(rs.getTimestamp("cp_updated_at"))));
            }

            final long laId = rs.getLong("availability_id");
            if (!rs.wasNull() && !availabilities.containsKey(laId)) {
                availabilities.put(laId, new ListingAvailability(
                        laId,
                        rs.getLong("listing_id"),
                        JdbcDateTimeUtils.readOffsetDateTime(rs, "start_date"),
                        JdbcDateTimeUtils.readOffsetDateTime(rs, "end_date"),
                        JdbcDateTimeUtils.readOffsetDateTime(rs, "la_created_at"),
                        JdbcDateTimeUtils.readOffsetDateTime(rs, "la_updated_at")));
            }
        }

        if (listing == null) {
            return Optional.empty();
        }
        return Optional.of(new ListingDetail(listing, car, owner,
                new ArrayList<>(pictures.values()),
                new ArrayList<>(availabilities.values())));
    };

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Autowired
    public ListingJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
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
    public Optional<ListingDetail> getListingDetailById(final long id) {
        return jdbcTemplate.query(
                "SELECT l.id AS listing_id, l.title, l.created_at AS listing_created_at, "
                        + "l.updated_at AS listing_updated_at, l.status, l.day_price, l.start_point, l.description, "
                        + "c.id AS car_id, c.owner_id, c.plate, c.brand, c.model, c.type, c.transmission, c.powertrain, "
                        + "u.id AS owner_user_id, u.email AS owner_email, u.forename AS owner_forename, u.surname AS owner_surname, "
                        + "cp.id AS car_picture_id, cp.image_id, cp.display_order, "
                        + "cp.created_at AS cp_created_at, cp.updated_at AS cp_updated_at, "
                        + "la.id AS availability_id, la.start_date, la.end_date, "
                        + "la.created_at AS la_created_at, la.updated_at AS la_updated_at "
                        + "FROM listings l "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "JOIN users u ON u.id = c.owner_id "
                        + "LEFT JOIN car_pictures cp ON cp.car_id = c.id "
                        + "LEFT JOIN listing_availability la ON la.listing_id = l.id "
                        + "WHERE l.id = ? "
                        + "ORDER BY cp.display_order ASC, la.start_date ASC",
                LISTING_DETAIL_EXTRACTOR, id);
    }

    @Override
    public List<Listing> getAllListings() {
        return jdbcTemplate.query("SELECT * FROM listings ORDER BY created_at DESC", LISTING_ROW_MAPPER);
    }

    @Override
    public List<Listing> searchListings(final ListingSearchCriteria criteria) {
        final StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT l.* FROM listings l INNER JOIN cars c ON l.car_id = c.id WHERE l.status = 'active' ");
        final MapSqlParameterSource params = new MapSqlParameterSource();
        appendSearchFilters(sql, params, criteria);
        sql.append("ORDER BY l.created_at DESC");
        return namedParameterJdbcTemplate.query(sql.toString(), params, LISTING_ROW_MAPPER);
    }

    @Override
    public List<ListingCard> searchListingCards(final ListingSearchCriteria criteria) {
        final StringBuilder sql = new StringBuilder(
                "SELECT l.id AS listing_id, c.brand, c.model, l.day_price, "
                        + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                        + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id "
                        + "FROM listings l INNER JOIN cars c ON l.car_id = c.id WHERE l.status = 'active' ");
        final MapSqlParameterSource params = new MapSqlParameterSource();
        appendSearchFilters(sql, params, criteria);
        sql.append("ORDER BY l.created_at DESC");
        return namedParameterJdbcTemplate.query(sql.toString(), params, LISTING_CARD_ROW_MAPPER);
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
    public List<ListingCard> getCheapestListingCards(final int limit) {
        return jdbcTemplate.query(
                "SELECT l.id AS listing_id, l.day_price, c.brand, c.model, "
                        + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                        + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id "
                        + "FROM listings l JOIN cars c ON c.id = l.car_id "
                        + "WHERE l.status = 'active' ORDER BY l.day_price ASC LIMIT ?",
                LISTING_CARD_ROW_MAPPER, limit);
    }

    @Override
    public List<ListingCard> getMostRecentListingCards(final int limit) {
        return jdbcTemplate.query(
                "SELECT l.id AS listing_id, l.day_price, c.brand, c.model, "
                        + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                        + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id "
                        + "FROM listings l JOIN cars c ON c.id = l.car_id "
                        + "WHERE l.status = 'active' ORDER BY l.created_at DESC LIMIT ?",
                LISTING_CARD_ROW_MAPPER, limit);
    }

    @Override
    public HomeListingCards getHomeListingCards(final int limit) {
        final String sql = "(SELECT :cheapestSection AS home_section, l.id AS listing_id, c.brand, c.model, l.day_price, "
                + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id "
                + "FROM listings l JOIN cars c ON c.id = l.car_id WHERE l.status = 'active' "
                + "ORDER BY l.day_price ASC LIMIT :homeLimit) "
                + "UNION ALL "
                + "(SELECT :recentSection AS home_section, l.id AS listing_id, c.brand, c.model, l.day_price, "
                + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id "
                + "FROM listings l JOIN cars c ON c.id = l.car_id WHERE l.status = 'active' "
                + "ORDER BY l.created_at DESC LIMIT :homeLimit)";

        final MapSqlParameterSource homeParams = new MapSqlParameterSource()
                .addValue("cheapestSection", HOME_SECTION_CHEAPEST)
                .addValue("recentSection", HOME_SECTION_RECENT)
                .addValue("homeLimit", limit);

        return namedParameterJdbcTemplate.query(
                sql,
                homeParams,
                rs -> {
                    final List<ListingCard> cheapest = new ArrayList<>();
                    final List<ListingCard> mostRecent = new ArrayList<>();
                    while (rs.next()) {
                        final ListingCard card = new ListingCard(
                                rs.getLong("listing_id"),
                                rs.getString("brand"),
                                rs.getString("model"),
                                rs.getBigDecimal("day_price"),
                                rs.getLong("image_id"));
                        final String section = rs.getString("home_section");
                        if (HOME_SECTION_CHEAPEST.equals(section)) {
                            cheapest.add(card);
                        } else if (HOME_SECTION_RECENT.equals(section)) {
                            mostRecent.add(card);
                        }
                    }
                    return new HomeListingCards(List.copyOf(cheapest), List.copyOf(mostRecent));
                });
    }

    @Override
    public List<ListingCard> findSimilarListingCards(final long listingId, final int limit) {
        final String sql = "SELECT l.id AS listing_id, l.day_price, c.brand, c.model, "
                + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id "
                + "FROM listings l "
                + "INNER JOIN cars c ON l.car_id = c.id "
                + "INNER JOIN listings la ON la.id = :listingId "
                + "INNER JOIN cars ca ON ca.id = la.car_id "
                + "WHERE l.status = 'active' "
                + "AND l.id <> :listingId "
                + "AND c.type = ca.type "
                + "AND c.powertrain = ca.powertrain "
                + "AND c.transmission = ca.transmission "
                + "ORDER BY l.created_at DESC "
                + "LIMIT :similarLimit";

        final MapSqlParameterSource similarParams = new MapSqlParameterSource()
                .addValue("listingId", listingId)
                .addValue("similarLimit", limit);

        return namedParameterJdbcTemplate.query(sql, similarParams, LISTING_CARD_ROW_MAPPER);
    }

    private static void appendSearchFilters(
            final StringBuilder sql,
            final MapSqlParameterSource params,
            final ListingSearchCriteria criteria) {
        if (criteria.getQuery() != null) {
            final String q = "%" + escapeLike(criteria.getQuery()) + "%";
            sql.append("AND (c.brand ILIKE :search ESCAPE '\\' OR c.model ILIKE :search ESCAPE '\\' OR l.title ILIKE :search ESCAPE '\\' ")
                    .append("OR l.description ILIKE :search ESCAPE '\\' OR l.start_point ILIKE :search ESCAPE '\\') ");
            params.addValue("search", q);
        }

        final boolean hasTrans = !criteria.getTransmissions().isEmpty();
        final boolean hasPwr = !criteria.getPowertrains().isEmpty();
        if (hasTrans || hasPwr) {
            sql.append("AND (");
            if (hasTrans) {
                sql.append("c.transmission IN (:transmissions)");
                params.addValue("transmissions", criteria.getTransmissions());
                if (hasPwr) {
                    sql.append(" OR ");
                }
            }
            if (hasPwr) {
                sql.append("c.powertrain IN (:powertrains)");
                params.addValue("powertrains", criteria.getPowertrains());
            }
            sql.append(") ");
        }

        if (!criteria.getCarTypes().isEmpty()) {
            sql.append("AND c.type IN (:carTypes) ");
            params.addValue("carTypes", criteria.getCarTypes());
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
                    .append("AND la.start_date <= :availEnd AND la.end_date >= :availStart) ");
            params.addValue("availStart", fromStart);
            params.addValue("availEnd", untilTs);

            sql.append("AND NOT EXISTS (")
                    .append("SELECT 1 FROM reservations r WHERE r.listing_id = l.id ")
                    .append("AND r.status IN ('accepted', 'started') ")
                    .append("AND r.start_date < :resWindowEnd AND r.end_date > :resWindowStart) ");
            params.addValue("resWindowEnd", untilTs);
            params.addValue("resWindowStart", fromStart);
        }
    }

    private static String escapeLike(final String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
