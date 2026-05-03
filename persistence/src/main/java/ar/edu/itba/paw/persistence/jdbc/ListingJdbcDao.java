package ar.edu.itba.paw.persistence.jdbc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarPicture;
import ar.edu.itba.paw.models.dto.HomeListingCards;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.dto.ListingCard;
import ar.edu.itba.paw.models.dto.ListingDetail;
import ar.edu.itba.paw.models.pagination.DualLayerPageWindow;
import ar.edu.itba.paw.models.util.ListingSearchCriteria;
import ar.edu.itba.paw.models.util.OwnerListingSearchCriteria;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.persistence.ListingDao;
import ar.edu.itba.paw.persistence.util.JdbcDateTimeUtils;

@Repository
public class ListingJdbcDao implements ListingDao {

    private static final String HOME_SECTION_CHEAPEST = "C";
    private static final String HOME_SECTION_RECENT = "R";

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "price",  "l.day_price",
            "date",   "l.created_at",
            "rating", "l.rating_avg"
    );
    private static final String DEFAULT_ORDER_BY = "l.created_at DESC";

    private static Long readNullableLongCol(final java.sql.ResultSet rs, final String column) throws java.sql.SQLException {
        final long v = rs.getLong(column);
        return rs.wasNull() ? null : v;
    }

    private static BigDecimal readNullableRatingAvg(final java.sql.ResultSet rs, final String column) throws java.sql.SQLException {
        final BigDecimal v = rs.getBigDecimal(column);
        if (rs.wasNull() || v == null) {
            return null;
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static Listing.Status readNullableListingStatus(final java.sql.ResultSet rs, final String column)
            throws java.sql.SQLException {
        final String raw = rs.getString(column);
        if (raw == null || rs.wasNull()) {
            return null;
        }
        return Listing.Status.valueOf(raw.toUpperCase());
    }

    private static final RowMapper<Listing> LISTING_ROW_MAPPER = (rs, rowNum) -> Listing.builder()
            .id(rs.getLong("id"))
            .title(rs.getString("title"))
            .carId(rs.getLong("car_id"))
            .createdAt(JdbcDateTimeUtils.readOffsetDateTime(rs, "created_at"))
            .updatedAt(JdbcDateTimeUtils.readOffsetDateTime(rs, "updated_at"))
            .status(Listing.Status.valueOf(rs.getString("status").toUpperCase()))
            .dayPrice(rs.getBigDecimal("day_price"))
            .startPointStreet(rs.getString("start_point_street"))
            .startPointNumber(rs.getString("start_point_number"))
            .description(rs.getString("description"))
            .checkInTime(JdbcDateTimeUtils.readLocalTime(rs, "check_in_time"))
            .checkOutTime(JdbcDateTimeUtils.readLocalTime(rs, "check_out_time"))
            .neighborhoodId(readNullableLongCol(rs, "neighborhood_id"))
            .ratingAvg(readNullableRatingAvg(rs, "rating_avg"))
            .build();

    private static final RowMapper<ListingCard> LISTING_CARD_ROW_MAPPER = (rs, rowNum) -> new ListingCard(
            rs.getLong("listing_id"),
            rs.getString("brand"),
            rs.getString("model"),
            rs.getBigDecimal("day_price"),
            rs.getLong("image_id"),
            readNullableRatingAvg(rs, "rating_avg"),
            readNullableListingStatus(rs, "listing_status"),
            rs.getLong("review_count")
    );

    private static final ResultSetExtractor<Optional<ListingDetail>> LISTING_DETAIL_EXTRACTOR = rs -> {
        Listing listing = null;
        Car car = null;
        User owner = null;
        final LinkedHashMap<Long, CarPicture> pictures = new LinkedHashMap<>();
        final LinkedHashMap<Long, ListingAvailability> availabilities = new LinkedHashMap<>();

        while (rs.next()) {
            if (listing == null) {
                listing = Listing.builder()
                        .id(rs.getLong("listing_id"))
                        .title(rs.getString("title"))
                        .carId(rs.getLong("car_id"))
                        .createdAt(JdbcDateTimeUtils.readOffsetDateTime(rs, "listing_created_at"))
                        .updatedAt(JdbcDateTimeUtils.readOffsetDateTime(rs, "listing_updated_at"))
                        .status(Listing.Status.valueOf(rs.getString("status").toUpperCase()))
                        .dayPrice(rs.getBigDecimal("day_price"))
                        .startPointStreet(rs.getString("start_point_street"))
                        .startPointNumber(rs.getString("start_point_number"))
                        .description(rs.getString("description"))
                        .checkInTime(JdbcDateTimeUtils.readLocalTime(rs, "check_in_time"))
                        .checkOutTime(JdbcDateTimeUtils.readLocalTime(rs, "check_out_time"))
                        .neighborhoodId(readNullableLongCol(rs, "neighborhood_id"))
                        .ratingAvg(readNullableRatingAvg(rs, "rating_avg"))
                        .build();
                car = Car.builder()
                        .id(rs.getLong("car_id"))
                        .ownerId(rs.getLong("owner_id"))
                        .plate(rs.getString("plate"))
                        .brand(rs.getString("brand"))
                        .model(rs.getString("model"))
                        .type(Car.Type.valueOf(rs.getString("type")))
                        .powertrain(Car.Powertrain.valueOf(rs.getString("powertrain")))
                        .transmission(Car.Transmission.valueOf(rs.getString("transmission")))
                        .build();
                owner = User.builder()
                        .id(rs.getLong("owner_user_id"))
                        .email(rs.getString("owner_email"))
                        .forename(rs.getString("owner_forename"))
                        .surname(rs.getString("owner_surname"))
                        .cbu(rs.getString("owner_cbu"))
                        .build();
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

    private final boolean listingsHasLegacyStartPointColumn;

    @Autowired
    public ListingJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.listingsHasLegacyStartPointColumn = detectListingsLegacyStartPointColumn(this.jdbcTemplate);
        this.jdbcInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("listings")
                .usingGeneratedKeyColumns("id");
    }

    private static boolean detectListingsLegacyStartPointColumn(final JdbcTemplate jdbcTemplate) {
        try {
            return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                    "SELECT EXISTS ("
                            + "SELECT 1 FROM information_schema.columns "
                            + "WHERE table_schema = current_schema() "
                            + "AND table_name = 'listings' AND column_name = 'start_point')",
                    Boolean.class));
        } catch (final DataAccessException e) {
            return false;
        }
    }

    @Override
    public Listing createListing(
            final long carId,
            final String title,
            final Listing.Status status,
            final BigDecimal dayPrice,
            final String startPointStreet,
            final String startPointNumber,
            final String description,
            final LocalTime checkInTime,
            final LocalTime checkOutTime,
            final Long neighborhoodId) {
        final Timestamp now = JdbcDateTimeUtils.nowTimestamp();
        final Map<String, Object> values = new HashMap<>();
        values.put("title", title);
        values.put("car_id", carId);
        values.put("created_at", now);
        values.put("updated_at", now);
        values.put("status", status.name().toLowerCase());
        values.put("day_price", dayPrice);
        values.put("start_point_street", startPointStreet);
        values.put("start_point_number", startPointNumber);
        if (listingsHasLegacyStartPointColumn) {
            values.put("start_point", startPointStreet);
        }
        values.put("description", description);
        values.put("check_in_time", java.sql.Time.valueOf(checkInTime));
        values.put("check_out_time", java.sql.Time.valueOf(checkOutTime));
        values.put("neighborhood_id", neighborhoodId);
        final Number id = jdbcInsert.executeAndReturnKey(values);

        return Listing.builder()
                .id(id.longValue())
                .title(title)
                .carId(carId)
                .createdAt(JdbcDateTimeUtils.toOffsetDateTime(now))
                .updatedAt(JdbcDateTimeUtils.toOffsetDateTime(now))
                .status(status)
                .dayPrice(dayPrice)
                .startPointStreet(startPointStreet)
                .startPointNumber(startPointNumber)
                .description(description)
                .checkInTime(checkInTime)
                .checkOutTime(checkOutTime)
                .neighborhoodId(neighborhoodId)
                .ratingAvg(null)
                .build();
    }

    @Override
    public Optional<Listing> getListingById(final long id) {
        return jdbcTemplate.query("SELECT * FROM listings WHERE id = ?", LISTING_ROW_MAPPER, id).stream().findAny();
    }

    @Override
    public Map<Long, LocalTime> findCheckInTimeByListingIds(final Collection<Long> listingIds) {
        if (listingIds == null || listingIds.isEmpty()) {
            return Map.of();
        }
        final MapSqlParameterSource params = new MapSqlParameterSource("ids", listingIds);
        return namedParameterJdbcTemplate.query(
                "SELECT id, check_in_time FROM listings WHERE id IN (:ids)",
                params,
                rs -> {
                    final Map<Long, LocalTime> out = new HashMap<>();
                    while (rs.next()) {
                        LocalTime t = JdbcDateTimeUtils.readLocalTime(rs, "check_in_time");
                        if (t == null) {
                            t = Listing.DEFAULT_CHECK_IN_TIME;
                        }
                        out.put(rs.getLong("id"), t);
                    }
                    return out;
                });
    }

    @Override
    public Optional<ListingDetail> getListingDetailById(final long id) {
        return jdbcTemplate.query(
                "SELECT l.id AS listing_id, l.title, l.created_at AS listing_created_at, "
                        + "l.updated_at AS listing_updated_at, l.status, l.day_price, l.start_point_street, l.start_point_number, l.description, "
                        + "l.check_in_time, l.check_out_time, l.neighborhood_id, l.rating_avg, "
                        + "c.id AS car_id, c.owner_id, c.plate, c.brand, c.model, c.type, c.transmission, c.powertrain, "
                        + "u.id AS owner_user_id, u.email AS owner_email, u.forename AS owner_forename, u.surname AS "
                        + "owner_surname, u.cbu AS owner_cbu, "
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
    public boolean updateOwnerListing(
            final long ownerId,
            final long listingId,
            final BigDecimal dayPrice,
            final String startPointStreet,
            final String startPointNumber,
            final String description,
            final LocalTime checkInTime,
            final LocalTime checkOutTime,
            final Long neighborhoodId) {
        final int updated = jdbcTemplate.update(
                "UPDATE listings l SET day_price = ?, start_point_street = ?, start_point_number = ?, description = ?, check_in_time = ?, "
                        + "check_out_time = ?, neighborhood_id = ?, updated_at = ? "
                        + "WHERE l.id = ? AND EXISTS ("
                        + "SELECT 1 FROM cars c WHERE c.id = l.car_id AND c.owner_id = ?)",
                dayPrice,
                startPointStreet,
                startPointNumber,
                description,
                java.sql.Time.valueOf(checkInTime),
                java.sql.Time.valueOf(checkOutTime),
                neighborhoodId,
                JdbcDateTimeUtils.nowTimestamp(),
                listingId,
                ownerId);
        return updated > 0;
    }

    @Override
    public boolean toggleListingStatus(final long ownerId, final long listingId) {
        final int updated = jdbcTemplate.update(
                "UPDATE listings l SET status = CASE WHEN l.status = 'active' THEN 'paused' "
                        + "WHEN l.status = 'paused' THEN 'active' ELSE l.status END, updated_at = ? "
                        + "WHERE l.id = ? AND EXISTS (SELECT 1 FROM cars c WHERE c.id = l.car_id AND c.owner_id = ?) "
                        + "AND l.status IN ('active','paused')",
                JdbcDateTimeUtils.nowTimestamp(),
                listingId,
                ownerId);
        return updated > 0;
    }

    @Override
    public boolean updateListingStatus(
            final long listingId,
            final Listing.Status newStatus,
            final Listing.Status... allowedFrom) {
        if (allowedFrom.length == 0) {
            return false;
        }
        final String placeholders = String.join(",", Collections.nCopies(allowedFrom.length, "?"));
        final List<Object> args = new ArrayList<>();
        args.add(newStatus.name().toLowerCase());
        args.add(JdbcDateTimeUtils.nowTimestamp());
        args.add(listingId);
        for (final Listing.Status s : allowedFrom) {
            args.add(s.name().toLowerCase());
        }
        final int updated = jdbcTemplate.update(
                "UPDATE listings SET status = ?, updated_at = ? WHERE id = ? AND status IN (" + placeholders + ")",
                args.toArray());
        return updated > 0;
    }

    @Override
    public List<Long> findListingIdsWithStatuses(final Listing.Status... statuses) {
        if (statuses.length == 0) {
            return List.of();
        }
        final String placeholders = String.join(",", Collections.nCopies(statuses.length, "?"));
        final Object[] args = new Object[statuses.length];
        for (int i = 0; i < statuses.length; i++) {
            args[i] = statuses[i].name().toLowerCase();
        }
        return jdbcTemplate.queryForList(
                "SELECT id FROM listings WHERE status IN (" + placeholders + ") ORDER BY id",
                Long.class,
                args);
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

    private static String listingCardSearchSelectClause() {
        return "SELECT l.id AS listing_id, c.brand, c.model, l.day_price, l.rating_avg, l.status AS listing_status, "
                + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id, "
                + buildReviewCountSql() + " AS review_count ";
    }

    private static String buildReviewCountSql() {
        return "(SELECT COUNT(DISTINCT r.id) FROM reservations r "
                + "INNER JOIN reviews rv ON r.id = rv.reservation_id "
                + "WHERE r.listing_id = l.id)";
    }

    private void appendListingCardSearchFromWhere(
            final StringBuilder sql,
            final MapSqlParameterSource params,
            final ListingSearchCriteria criteria) {
        sql.append("FROM listings l INNER JOIN cars c ON l.car_id = c.id WHERE l.status = 'active' ");
        appendSearchFilters(sql, params, criteria);
    }

    @Override
    public Page<ListingCard> searchListingCards(final ListingSearchCriteria criteria) {
        final DualLayerPageWindow w = DualLayerPageWindow.compute(
                criteria.getPage(), criteria.getUiPageSize(), criteria.getDbFetchSize());
        if (criteria.hasAvailabilityRange()) {
            final MapSqlParameterSource params = new MapSqlParameterSource();
            final StringBuilder sql = new StringBuilder(listingCardSearchSelectClause());
            appendListingCardSearchFromWhere(sql, params, criteria);
            sql.append("ORDER BY ").append(buildOrderBy(criteria.getSortBy(), criteria.getSortDirection()));
            List<ListingCard> result = namedParameterJdbcTemplate.query(sql.toString(), params, LISTING_CARD_ROW_MAPPER);
            result = filterByAvailabilityCoverage(criteria, result, ListingCard::getListingId);
            final long total = result.size();
            final List<ListingCard> slice = DualLayerPageWindow.sliceGlobalOrdered(result, w);
            return new Page<>(slice, w.uiPage(), w.uiPageSize(), total);
        }

        final MapSqlParameterSource countParams = new MapSqlParameterSource();
        final StringBuilder countFromWhere = new StringBuilder();
        appendListingCardSearchFromWhere(countFromWhere, countParams, criteria);
        final String countSql = "SELECT COUNT(*) " + countFromWhere;
        final Long totalObj = namedParameterJdbcTemplate.queryForObject(countSql, countParams, Long.class);
        final long total = totalObj != null ? totalObj : 0L;

        final MapSqlParameterSource listParams = new MapSqlParameterSource(countParams.getValues());
        listParams.addValue("limit", w.sqlLimit());
        listParams.addValue("offset", w.sqlOffset());
        final StringBuilder listSql = new StringBuilder(listingCardSearchSelectClause());
        appendListingCardSearchFromWhere(listSql, listParams, criteria);
        listSql.append("ORDER BY ")
                .append(buildOrderBy(criteria.getSortBy(), criteria.getSortDirection()))
                .append(" LIMIT :limit OFFSET :offset");
        final List<ListingCard> batch = namedParameterJdbcTemplate.query(
                listSql.toString(), listParams, LISTING_CARD_ROW_MAPPER);
        final List<ListingCard> slice = DualLayerPageWindow.sliceBatch(batch, w);
        return new Page<>(slice, w.uiPage(), w.uiPageSize(), total);
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
    public List<Listing> findListingsByOwnerIdAndStatus(final long ownerId, final Listing.Status status) {
        return jdbcTemplate.query(
                "SELECT l.* FROM listings l INNER JOIN cars c ON l.car_id = c.id "
                        + "WHERE c.owner_id = ? AND LOWER(l.status) = ?",
                LISTING_ROW_MAPPER,
                ownerId,
                status.name().toLowerCase());
    }

    @Override
    public List<ListingCard> getCheapestListingCardsWindow(
            final int offset,
            final int limit,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId) {
        final MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset);
        appendPublicBrowseFilters(params, browseWallDate, excludeOwnerUserId);
        final String sql = "SELECT l.id AS listing_id, l.day_price, c.brand, c.model, l.rating_avg, "
                + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id, l.status AS listing_status, "
                + buildReviewCountSql() + " AS review_count "
                + "FROM listings l JOIN cars c ON c.id = l.car_id "
                + "WHERE l.status = 'active' "
                + publicBrowseAvailabilitySql(browseWallDate)
                + publicBrowseExcludeOwnerSql(excludeOwnerUserId)
                + "ORDER BY l.day_price ASC LIMIT :limit OFFSET :offset";
        return namedParameterJdbcTemplate.query(sql, params, LISTING_CARD_ROW_MAPPER);
    }

    @Override
    public List<ListingCard> getMostRecentListingCardsWindow(
            final int offset,
            final int limit,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId) {
        final MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset);
        appendPublicBrowseFilters(params, browseWallDate, excludeOwnerUserId);
        final String sql = "SELECT l.id AS listing_id, l.day_price, c.brand, c.model, l.rating_avg, "
                + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id, l.status AS listing_status, "
                + buildReviewCountSql() + " AS review_count "
                + "FROM listings l JOIN cars c ON c.id = l.car_id "
                + "WHERE l.status = 'active' "
                + publicBrowseAvailabilitySql(browseWallDate)
                + publicBrowseExcludeOwnerSql(excludeOwnerUserId)
                + "ORDER BY l.created_at DESC LIMIT :limit OFFSET :offset";
        return namedParameterJdbcTemplate.query(sql, params, LISTING_CARD_ROW_MAPPER);
    }

    @Override
    public Page<ListingCard> getOwnerListingCards(final OwnerListingSearchCriteria criteria) {
        final int page = criteria.getPage();
        final int pageSize = criteria.getPageSize();
        final MapSqlParameterSource countParams = new MapSqlParameterSource("ownerId", criteria.getOwnerId());
        final StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM listings l JOIN cars c ON c.id = l.car_id WHERE c.owner_id = :ownerId ");
        appendOwnerListingFilters(countSql, countParams, criteria);
        final Long total = namedParameterJdbcTemplate.queryForObject(countSql.toString(), countParams, Long.class);
        final int offset = page * pageSize;
        final MapSqlParameterSource listParams = new MapSqlParameterSource("ownerId", criteria.getOwnerId())
                .addValue("limit", pageSize)
                .addValue("offset", offset);
        final StringBuilder listSql = new StringBuilder(
                "SELECT l.id AS listing_id, l.day_price, c.brand, c.model, l.rating_avg, "
                        + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                        + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id, l.status AS listing_status, "
                        + buildReviewCountSql() + " AS review_count "
                        + "FROM listings l JOIN cars c ON c.id = l.car_id "
                        + "WHERE c.owner_id = :ownerId ");
        appendOwnerListingFilters(listSql, listParams, criteria);
        listSql.append("ORDER BY ").append(buildOrderBy(criteria.getSortBy(), criteria.getSortDirection()))
               .append(" LIMIT :limit OFFSET :offset");
        final List<ListingCard> content = namedParameterJdbcTemplate.query(listSql.toString(), listParams, LISTING_CARD_ROW_MAPPER);
        return new Page<>(content, page, pageSize, total != null ? total : 0L);
    }

    private static void appendOwnerListingFilters(
            final StringBuilder sql,
            final MapSqlParameterSource params,
            final OwnerListingSearchCriteria criteria) {
        if (!criteria.getListingStatusFilters().isEmpty()) {
            sql.append("AND LOWER(l.status) IN (:ownerListingStatuses) ");
            params.addValue("ownerListingStatuses", criteria.getListingStatusFilters());
        }
        final String textQuery = criteria.getTextQuery();
        if (textQuery != null) {
            final String q = "%" + escapeLike(textQuery) + "%";
            sql.append("AND (LOWER(c.brand) LIKE LOWER(:ownerListingSearch) ESCAPE '\\' "
                    + "OR LOWER(c.model) LIKE LOWER(:ownerListingSearch) ESCAPE '\\' "
                    + "OR LOWER(l.title) LIKE LOWER(:ownerListingSearch) ESCAPE '\\') ");
            params.addValue("ownerListingSearch", q);
        }
        if (!criteria.getCarTypes().isEmpty()) {
            sql.append("AND c.type IN (:ownerCarTypes) ");
            params.addValue("ownerCarTypes", criteria.getCarTypes());
        }
        if (!criteria.getTransmissions().isEmpty()) {
            sql.append("AND c.transmission IN (:ownerTransmissions) ");
            params.addValue("ownerTransmissions", criteria.getTransmissions());
        }
        if (!criteria.getPowertrains().isEmpty()) {
            sql.append("AND c.powertrain IN (:ownerPowertrains) ");
            params.addValue("ownerPowertrains", criteria.getPowertrains());
        }
        if (criteria.getMinPrice() != null) {
            sql.append("AND l.day_price >= :ownerMinPrice ");
            params.addValue("ownerMinPrice", criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() != null) {
            sql.append("AND l.day_price <= :ownerMaxPrice ");
            params.addValue("ownerMaxPrice", criteria.getMaxPrice());
        }
        appendRatingBandFilter(sql, criteria.getRatingBands());
    }

    @Override
    public boolean hasListingsByOwner(final long ownerId) {
        final Integer exists = jdbcTemplate.queryForObject(
                "SELECT CASE WHEN EXISTS ("
                        + "SELECT 1 FROM listings l JOIN cars c ON c.id = l.car_id WHERE c.owner_id = ?"
                        + ") THEN 1 ELSE 0 END",
                Integer.class,
                ownerId);
        return exists != null && exists == 1;
    }

    private long countActiveListings() {
        final Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM listings WHERE status = 'active'", Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public long countBrowseEligibleActiveListings(final LocalDate browseWallDate, final Long excludeOwnerUserId) {
        if (browseWallDate == null && excludeOwnerUserId == null) {
            return countActiveListings();
        }
        final MapSqlParameterSource params = new MapSqlParameterSource();
        appendPublicBrowseFilters(params, browseWallDate, excludeOwnerUserId);
        final String sql = "SELECT COUNT(DISTINCT l.id) FROM listings l INNER JOIN cars c ON c.id = l.car_id "
                + "WHERE l.status = 'active' "
                + publicBrowseAvailabilitySql(browseWallDate)
                + publicBrowseExcludeOwnerSql(excludeOwnerUserId);
        final Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    private static void appendPublicBrowseFilters(
            final MapSqlParameterSource params,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId) {
        if (browseWallDate != null) {
            params.addValue("browseWallDate", JdbcDateTimeUtils.toSqlDate(browseWallDate));
        }
        if (excludeOwnerUserId != null) {
            params.addValue("excludeOwnerUserId", excludeOwnerUserId);
        }
    }

    private static String publicBrowseAvailabilitySql(final LocalDate browseWallDate) {
        if (browseWallDate == null) {
            return "";
        }
        return "AND EXISTS (SELECT 1 FROM listing_availability la_pub WHERE la_pub.listing_id = l.id "
                + "AND la_pub.end_date >= :browseWallDate) ";
    }

    private static String publicBrowseExcludeOwnerSql(final Long excludeOwnerUserId) {
        if (excludeOwnerUserId == null) {
            return "";
        }
        return "AND c.owner_id <> :excludeOwnerUserId ";
    }

    @Override
    public HomeListingCards getHomeListingCards(
            final int limit,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId) {
        final MapSqlParameterSource homeParams = new MapSqlParameterSource()
                .addValue("cheapestSection", HOME_SECTION_CHEAPEST)
                .addValue("recentSection", HOME_SECTION_RECENT)
                .addValue("homeLimit", limit);
        appendPublicBrowseFilters(homeParams, browseWallDate, excludeOwnerUserId);
        final String sql = "(SELECT :cheapestSection AS home_section, l.id AS listing_id, c.brand, c.model, l.day_price, l.rating_avg, "
                + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id, l.status AS listing_status, "
                + buildReviewCountSql() + " AS review_count "
                + "FROM listings l JOIN cars c ON c.id = l.car_id WHERE l.status = 'active' "
                + publicBrowseAvailabilitySql(browseWallDate)
                + publicBrowseExcludeOwnerSql(excludeOwnerUserId)
                + "ORDER BY l.day_price ASC LIMIT :homeLimit) "
                + "UNION ALL "
                + "(SELECT :recentSection AS home_section, l.id AS listing_id, c.brand, c.model, l.day_price, l.rating_avg, "
                + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id, l.status AS listing_status, "
                + buildReviewCountSql() + " AS review_count "
                + "FROM listings l JOIN cars c ON c.id = l.car_id WHERE l.status = 'active' "
                + publicBrowseAvailabilitySql(browseWallDate)
                + publicBrowseExcludeOwnerSql(excludeOwnerUserId)
                + "ORDER BY l.created_at DESC LIMIT :homeLimit)";

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
                                rs.getLong("image_id"),
                                readNullableRatingAvg(rs, "rating_avg"),
                                readNullableListingStatus(rs, "listing_status"),
                                rs.getLong("review_count"));
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
    public List<ListingCard> findSimilarListingCards(
            final long listingId,
            final int limit,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId) {
        final MapSqlParameterSource similarParams = new MapSqlParameterSource()
                .addValue("listingId", listingId)
                .addValue("similarLimit", limit);
        appendPublicBrowseFilters(similarParams, browseWallDate, excludeOwnerUserId);
        final String sql = "SELECT l.id AS listing_id, l.day_price, c.brand, c.model, l.rating_avg, "
                + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id, l.status AS listing_status, "
                + buildReviewCountSql() + " AS review_count "
                + "FROM listings l "
                + "INNER JOIN cars c ON l.car_id = c.id "
                + "INNER JOIN listings la ON la.id = :listingId "
                + "INNER JOIN cars ca ON ca.id = la.car_id "
                + "WHERE l.status = 'active' "
                + "AND l.id <> :listingId "
                + "AND c.type = ca.type "
                + "AND c.powertrain = ca.powertrain "
                + "AND c.transmission = ca.transmission "
                + publicBrowseAvailabilitySql(browseWallDate)
                + publicBrowseExcludeOwnerSql(excludeOwnerUserId)
                + "ORDER BY l.created_at DESC "
                + "LIMIT :similarLimit";

        return namedParameterJdbcTemplate.query(sql, similarParams, LISTING_CARD_ROW_MAPPER);
    }

    private static String buildOrderBy(final String sortBy, final String sortDirection) {
        final String col = SORT_COLUMNS.getOrDefault(sortBy, "l.created_at");
        final String dir = "asc".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        if ("rating".equals(sortBy)) {
            return col + " " + dir + " NULLS LAST, l.created_at DESC";
        }
        return col + " " + dir;
    }

    private static void appendSearchFilters(
            final StringBuilder sql,
            final MapSqlParameterSource params,
            final ListingSearchCriteria criteria) {
        if (criteria.getQuery() != null) {
            final String q = "%" + escapeLike(criteria.getQuery()) + "%";
            sql.append("AND (LOWER(c.brand) LIKE LOWER(:search) ESCAPE '\\' OR LOWER(c.model) LIKE LOWER(:search) ESCAPE '\\' ")
                    .append("OR LOWER(l.title) LIKE LOWER(:search) ESCAPE '\\' OR LOWER(l.description) LIKE LOWER(:search) ESCAPE '\\' ")
                    .append("OR LOWER(CONCAT(COALESCE(l.start_point_street, ''), ' ', COALESCE(l.start_point_number, ''))) "
                            + "LIKE LOWER(:search) ESCAPE '\\') ");
            params.addValue("search", q);
        }

        if (!criteria.getTransmissions().isEmpty()) {
            sql.append("AND c.transmission IN (:transmissions) ");
            params.addValue("transmissions", criteria.getTransmissions());
        }
        if (!criteria.getPowertrains().isEmpty()) {
            sql.append("AND c.powertrain IN (:powertrains) ");
            params.addValue("powertrains", criteria.getPowertrains());
        }

        if (!criteria.getNeighborhoodIds().isEmpty()) {
            sql.append("AND l.neighborhood_id IN (:searchNeighborhoodIds) ");
            params.addValue("searchNeighborhoodIds", criteria.getNeighborhoodIds());
        }

        if (!criteria.getCarTypes().isEmpty()) {
            sql.append("AND c.type IN (:carTypes) ");
            params.addValue("carTypes", criteria.getCarTypes());
        }

        if (criteria.getMinPrice() != null) {
            sql.append("AND l.day_price >= :minPrice ");
            params.addValue("minPrice", criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() != null) {
            sql.append("AND l.day_price <= :maxPrice ");
            params.addValue("maxPrice", criteria.getMaxPrice());
        }

        appendRatingBandFilter(sql, criteria.getRatingBands());

        if (criteria.hasAvailabilityRange()) {
            final Instant fromInstant = criteria.getAvailabilityRangeStart();
            final Instant untilExclusive = criteria.getAvailabilityRangeEndExclusive();
            sql.append("AND NOT EXISTS (")
                    .append("SELECT 1 FROM reservations r WHERE r.listing_id = l.id ")
                    .append("AND r.status IN ('pending', 'accepted', 'started') ")
                    .append("AND r.start_date < :resWindowEnd AND r.end_date > :resWindowStart) ");
            params.addValue("resWindowEnd", Timestamp.from(untilExclusive));
            params.addValue("resWindowStart", Timestamp.from(fromInstant));
        }
        appendPublicBrowseFilters(params, criteria.getBrowseWallDate(), criteria.getExcludeOwnerUserId());
        sql.append(publicBrowseAvailabilitySql(criteria.getBrowseWallDate()));
        sql.append(publicBrowseExcludeOwnerSql(criteria.getExcludeOwnerUserId()));
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
                        + "WHERE listing_id IN (:listingIds) "
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

    private static void appendRatingBandFilter(final StringBuilder sql, final List<String> ratingBands) {
        if (ratingBands.isEmpty()) {
            return;
        }
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
