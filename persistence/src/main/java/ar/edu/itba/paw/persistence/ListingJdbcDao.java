package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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

import ar.edu.itba.paw.models.AvailabilityPeriod;
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
            rs.getString("description"),
            JdbcDateTimeUtils.readLocalTime(rs, "check_in_time"),
            JdbcDateTimeUtils.readLocalTime(rs, "check_out_time")
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
                        rs.getString("description"),
                        JdbcDateTimeUtils.readLocalTime(rs, "check_in_time"),
                        JdbcDateTimeUtils.readLocalTime(rs, "check_out_time"));
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
                        JdbcDateTimeUtils.readLocalDate(rs, "start_date"),
                        JdbcDateTimeUtils.readLocalDate(rs, "end_date"),
                        JdbcDateTimeUtils.readOffsetDateTime(rs, "la_created_at"),
                        JdbcDateTimeUtils.readOffsetDateTime(rs, "la_updated_at"),
                        rs.getBoolean("la_is_active")));
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
            final String description,
            final LocalTime checkInTime,
            final LocalTime checkOutTime) {
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
        values.put("check_in_time", java.sql.Time.valueOf(checkInTime));
        values.put("check_out_time", java.sql.Time.valueOf(checkOutTime));
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
                description,
                checkInTime,
                checkOutTime);
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
                        + "l.check_in_time, l.check_out_time, "
                        + "c.id AS car_id, c.owner_id, c.plate, c.brand, c.model, c.type, c.transmission, c.powertrain, "
                        + "u.id AS owner_user_id, u.email AS owner_email, u.forename AS owner_forename, u.surname AS owner_surname, "
                        + "cp.id AS car_picture_id, cp.image_id, cp.display_order, "
                        + "cp.created_at AS cp_created_at, cp.updated_at AS cp_updated_at, "
                        + "la.id AS availability_id, la.start_date, la.end_date, "
                        + "la.created_at AS la_created_at, la.updated_at AS la_updated_at, la.is_active AS la_is_active "
                        + "FROM listings l "
                        + "JOIN cars c ON c.id = l.car_id "
                        + "JOIN users u ON u.id = c.owner_id "
                        + "LEFT JOIN car_pictures cp ON cp.car_id = c.id "
                        + "LEFT JOIN listing_availability la ON la.listing_id = l.id AND la.is_active = TRUE "
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
        final List<Listing> result = namedParameterJdbcTemplate.query(sql.toString(), params, LISTING_ROW_MAPPER);
        return filterByAvailabilityCoverage(criteria, result, Listing::getId);
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
        final List<ListingCard> result = namedParameterJdbcTemplate.query(sql.toString(), params, LISTING_CARD_ROW_MAPPER);
        return filterByAvailabilityCoverage(criteria, result, ListingCard::getListingId);
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
            sql.append("AND (LOWER(c.brand) LIKE LOWER(:search) ESCAPE '\\' OR LOWER(c.model) LIKE LOWER(:search) ESCAPE '\\' ")
                    .append("OR LOWER(l.title) LIKE LOWER(:search) ESCAPE '\\' OR LOWER(l.description) LIKE LOWER(:search) ESCAPE '\\' ")
                    .append("OR LOWER(l.start_point) LIKE LOWER(:search) ESCAPE '\\') ");
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
            sql.append("AND NOT EXISTS (")
                    .append("SELECT 1 FROM reservations r WHERE r.listing_id = l.id ")
                    .append("AND r.status IN ('accepted', 'started') ")
                    .append("AND r.start_date < :resWindowEnd AND r.end_date > :resWindowStart) ");
            params.addValue("resWindowEnd", Timestamp.from(untilExclusive));
            params.addValue("resWindowStart", Timestamp.from(fromInstant));
        }
    }

    private <T> List<T> filterByAvailabilityCoverage(
            final ListingSearchCriteria criteria,
            final List<T> rows,
            final java.util.function.ToLongFunction<T> listingIdExtractor) {
        if (!criteria.hasAvailabilityRange() || rows.isEmpty()) {
            return rows;
        }
        final LocalDate fromDay = criteria.getAvailabilityRangeStart().atZone(AvailabilityPeriod.WALL_ZONE).toLocalDate();
        final LocalDate untilDay = criteria.getAvailabilityRangeEndExclusive().minusNanos(1)
                .atZone(AvailabilityPeriod.WALL_ZONE)
                .toLocalDate();
        final List<Long> listingIds = rows.stream()
                .mapToLong(listingIdExtractor)
                .distinct()
                .boxed()
                .toList();
        final Map<Long, List<DateRange>> rangesByListingId = loadAvailabilityRangesByListingId(listingIds, fromDay, untilDay);
        return rows.stream()
                .filter(row -> coversEveryDay(rangesByListingId.get(listingIdExtractor.applyAsLong(row)), fromDay, untilDay))
                .toList();
    }

    private Map<Long, List<DateRange>> loadAvailabilityRangesByListingId(
            final List<Long> listingIds,
            final LocalDate fromDay,
            final LocalDate untilDay) {
        if (listingIds.isEmpty()) {
            return Map.of();
        }

        final MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("listingIds", listingIds)
                .addValue("fromDay", JdbcDateTimeUtils.toSqlDate(fromDay))
                .addValue("untilDay", JdbcDateTimeUtils.toSqlDate(untilDay));

        final List<ListingDateRange> rows = namedParameterJdbcTemplate.query(
                "SELECT listing_id, start_date, end_date FROM listing_availability "
                        + "WHERE is_active = TRUE "
                        + "AND listing_id IN (:listingIds) "
                        + "AND start_date <= :untilDay "
                        + "AND end_date >= :fromDay "
                        + "ORDER BY listing_id ASC, start_date ASC, end_date ASC",
                params,
                (rs, rowNum) -> new ListingDateRange(
                        rs.getLong("listing_id"),
                        JdbcDateTimeUtils.readLocalDate(rs, "start_date"),
                        JdbcDateTimeUtils.readLocalDate(rs, "end_date")));

        final Map<Long, List<DateRange>> rangesByListingId = new HashMap<>();
        for (final ListingDateRange row : rows) {
            rangesByListingId.computeIfAbsent(row.listingId(), ignored -> new ArrayList<>())
                    .add(new DateRange(row.start(), row.end()));
        }
        return rangesByListingId;
    }

    private static boolean coversEveryDay(final List<DateRange> ranges, final LocalDate fromDay, final LocalDate untilDay) {
        if (ranges == null || ranges.isEmpty()) {
            return false;
        }

        LocalDate nextDayToCover = fromDay;
        for (final DateRange range : ranges) {
            if (range.end().isBefore(nextDayToCover)) {
                continue;
            }
            if (range.start().isAfter(nextDayToCover)) {
                return false;
            }
            if (!range.end().isBefore(untilDay)) {
                return true;
            }
            nextDayToCover = range.end().plusDays(1);
        }
        return nextDayToCover.isAfter(untilDay);
    }

    private record ListingDateRange(long listingId, LocalDate start, LocalDate end) {
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }

    private static String escapeLike(final String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
