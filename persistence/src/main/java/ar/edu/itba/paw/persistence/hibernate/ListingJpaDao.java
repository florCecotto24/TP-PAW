package ar.edu.itba.paw.persistence.hibernate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static ar.edu.itba.paw.persistence.util.JpaQueryUtils.bindParams;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarPicture;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.HomeListingCards;
import ar.edu.itba.paw.models.dto.ListingCard;
import ar.edu.itba.paw.models.dto.ListingDetail;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.pagination.DualLayerPageWindow;
import ar.edu.itba.paw.models.util.ListingSearchCriteria;
import ar.edu.itba.paw.models.util.OwnerListingSearchCriteria;
import ar.edu.itba.paw.persistence.ListingDao;

@Transactional
@Repository
public class ListingJpaDao implements ListingDao {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "price",  "l.day_price",
            "date",   "l.created_at",
            "rating", "l.rating_avg"
    );

    @PersistenceContext
    private EntityManager em;

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
        final Car carRef = em.getReference(Car.class, carId);
        final Neighborhood neighborhoodRef = neighborhoodId != null
                ? em.getReference(Neighborhood.class, neighborhoodId)
                : null;
        final OffsetDateTime now = OffsetDateTime.now();
        final Listing listing = Listing.builder()
                .car(carRef)
                .title(title)
                .status(status)
                .dayPrice(dayPrice)
                .startPointStreet(startPointStreet)
                .startPointNumber(startPointNumber)
                .description(description)
                .checkInTime(checkInTime)
                .checkOutTime(checkOutTime)
                .neighborhood(neighborhoodRef)
                .createdAt(now)
                .updatedAt(now)
                .ratingAvg(null)
                .build();
        em.persist(listing);
        return listing;
    }

    @Override
    public Optional<Listing> getListingById(final long id) {
        return Optional.ofNullable(em.find(Listing.class, id));
    }

    @Override
    public Map<Long, LocalTime> findCheckInTimeByListingIds(final Collection<Long> listingIds) {
        if (listingIds == null || listingIds.isEmpty()) {
            return Map.of();
        }
        return em.createQuery("FROM Listing l WHERE l.id IN :ids", Listing.class)
                .setParameter("ids", listingIds)
                .getResultList()
                .stream()
                .collect(Collectors.toMap(
                        Listing::getId,
                        l -> l.getCheckInTime() != null ? l.getCheckInTime() : Listing.DEFAULT_CHECK_IN_TIME));
    }

    @Override
    public Optional<ListingDetail> getListingDetailById(final long id) {
        final Listing listing = em.find(Listing.class, id);
        if (listing == null) {
            return Optional.empty();
        }
        final Car car = listing.getCar();
        final User owner = car.getOwner();
        final List<CarPicture> pictures = em.createQuery(
                        "FROM CarPicture cp WHERE cp.car.id = :carId ORDER BY cp.displayOrder ASC",
                        CarPicture.class)
                .setParameter("carId", car.getId())
                .getResultList();
        final List<ListingAvailability> availabilities = em.createQuery(
                        "FROM ListingAvailability la WHERE la.listing.id = :listingId ORDER BY la.startInclusive ASC",
                        ListingAvailability.class)
                .setParameter("listingId", id)
                .getResultList();
        return Optional.of(new ListingDetail(listing, car, owner, pictures, availabilities));
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
        final Listing listing = em.find(Listing.class, listingId);
        if (listing == null || listing.getCar().getOwner().getId() != ownerId) {
            return false;
        }
        listing.setDayPrice(dayPrice);
        listing.setStartPointStreet(startPointStreet);
        listing.setStartPointNumber(startPointNumber);
        listing.setDescription(description);
        listing.setCheckInTime(checkInTime);
        listing.setCheckOutTime(checkOutTime);
        listing.setNeighborhood(
                neighborhoodId != null ? em.getReference(Neighborhood.class, neighborhoodId) : null);
        listing.setUpdatedAt(OffsetDateTime.now());
        return true;
    }

    @Override
    public boolean toggleListingStatus(final long ownerId, final long listingId) {
        final Listing listing = em.find(Listing.class, listingId);
        if (listing == null || listing.getCar().getOwner().getId() != ownerId) {
            return false;
        }
        final Listing.Status current = listing.getStatus();
        if (current == Listing.Status.ACTIVE) {
            listing.setStatus(Listing.Status.PAUSED);
        } else if (current == Listing.Status.PAUSED) {
            listing.setStatus(Listing.Status.ACTIVE);
        } else {
            return false;
        }
        listing.setUpdatedAt(OffsetDateTime.now());
        return true;
    }

    @Override
    public boolean updateListingStatus(
            final long listingId,
            final Listing.Status newStatus,
            final Listing.Status... allowedFrom) {
        if (allowedFrom.length == 0) {
            return false;
        }
        final Listing listing = em.find(Listing.class, listingId);
        if (listing == null) {
            return false;
        }
        boolean allowed = false;
        for (final Listing.Status s : allowedFrom) {
            if (s == listing.getStatus()) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            return false;
        }
        listing.setStatus(newStatus);
        listing.setUpdatedAt(OffsetDateTime.now());
        return true;
    }

    @Override
    public List<Listing> findListingsWithStatuses(final Listing.Status... statuses) {
        if (statuses.length == 0) {
            return List.of();
        }
        return em.createQuery(
                        "FROM Listing l WHERE l.status IN :statuses ORDER BY l.id",
                        Listing.class)
                .setParameter("statuses", Arrays.asList(statuses))
                .getResultList();
    }

    @Override
    public List<Listing> getAllListings() {
        return em.createQuery("FROM Listing l ORDER BY l.createdAt DESC", Listing.class).getResultList();
    }

    @Override
    public List<Listing> searchListings(final ListingSearchCriteria criteria) {
        final StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT l.* FROM listings l INNER JOIN cars c ON l.car_id = c.id WHERE l.status = 'active' ");
        final Map<String, Object> params = new HashMap<>();
        appendSearchFilters(sql, params, criteria);
        sql.append("ORDER BY l.created_at DESC");
        final List<Listing> result = runEntityNativeQuery(sql.toString(), params, Listing.class);
        return filterByAvailabilityCoverage(criteria, result, Listing::getId);
    }

    @Override
    public List<Listing> getCheapestListings(final int limit) {
        return em.createQuery(
                        "FROM Listing l WHERE l.status = :status ORDER BY l.dayPrice ASC",
                        Listing.class)
                .setParameter("status", Listing.Status.ACTIVE)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public List<Listing> getMostRecentListings(final int limit) {
        return em.createQuery(
                        "FROM Listing l WHERE l.status = :status ORDER BY l.createdAt DESC",
                        Listing.class)
                .setParameter("status", Listing.Status.ACTIVE)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public List<Listing> findListingsByOwnerIdAndStatus(final long ownerId, final Listing.Status status) {
        return em.createQuery(
                        "FROM Listing l WHERE l.car.owner.id = :ownerId AND l.status = :status",
                        Listing.class)
                .setParameter("ownerId", ownerId)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public List<ListingCard> getCheapestListingCardsWindow(
            final int offset,
            final int limit,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("limit", limit);
        params.put("offset", offset);
        appendPublicBrowseFilterParams(params, browseWallDate, excludeOwnerUserId);
        final String sql = listingCardSelectPrefix()
                + "FROM listings l JOIN cars c ON c.id = l.car_id "
                + "WHERE l.status = 'active' "
                + publicBrowseAvailabilitySql(browseWallDate)
                + publicBrowseExcludeOwnerSql(excludeOwnerUserId)
                + "ORDER BY l.day_price ASC LIMIT :limit OFFSET :offset";
        return runListingCardNativeQuery(sql, params);
    }

    @Override
    public List<ListingCard> getMostRecentListingCardsWindow(
            final int offset,
            final int limit,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("limit", limit);
        params.put("offset", offset);
        appendPublicBrowseFilterParams(params, browseWallDate, excludeOwnerUserId);
        final String sql = listingCardSelectPrefix()
                + "FROM listings l JOIN cars c ON c.id = l.car_id "
                + "WHERE l.status = 'active' "
                + publicBrowseAvailabilitySql(browseWallDate)
                + publicBrowseExcludeOwnerSql(excludeOwnerUserId)
                + "ORDER BY l.created_at DESC LIMIT :limit OFFSET :offset";
        return runListingCardNativeQuery(sql, params);
    }

    @Override
    public Page<ListingCard> getOwnerListingCards(final OwnerListingSearchCriteria criteria) {
        final int page = criteria.getPage();
        final int pageSize = criteria.getPageSize();

        final Map<String, Object> countParams = new HashMap<>();
        countParams.put("ownerId", criteria.getOwnerId());
        final StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM listings l JOIN cars c ON c.id = l.car_id WHERE c.owner_id = :ownerId ");
        appendOwnerListingFilters(countSql, countParams, criteria);
        final Number total = (Number) bindParams(em.createNativeQuery(countSql.toString()), countParams).getSingleResult();

        final int offset = page * pageSize;
        final Map<String, Object> listParams = new HashMap<>();
        listParams.put("ownerId", criteria.getOwnerId());
        listParams.put("limit", pageSize);
        listParams.put("offset", offset);
        final StringBuilder listSql = new StringBuilder(
                "SELECT l.id AS listing_id, c.brand, c.model, l.day_price, "
                        + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                        + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id, l.rating_avg, l.status AS listing_status, "
                        + buildReviewCountSql() + " AS review_count "
                        + "FROM listings l JOIN cars c ON c.id = l.car_id "
                        + "WHERE c.owner_id = :ownerId ");
        appendOwnerListingFilters(listSql, listParams, criteria);
        listSql.append("ORDER BY ").append(buildOrderBy(criteria.getSortBy(), criteria.getSortDirection()))
               .append(" LIMIT :limit OFFSET :offset");
        final List<ListingCard> content = runListingCardNativeQuery(listSql.toString(), listParams);
        return new Page<>(content, page, pageSize, total != null ? total.longValue() : 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Listing> findActiveOrPausedListingByCar(final long carId) {
        return em.createQuery(
                        "FROM Listing l WHERE l.car.id = :carId AND l.status <> :finished ORDER BY l.id DESC",
                        Listing.class)
                .setParameter("carId", carId)
                .setParameter("finished", Listing.Status.FINISHED)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Listing> findMostRecentListingByCar(final long carId) {
        return em.createQuery(
                        "FROM Listing l WHERE l.car.id = :carId ORDER BY l.id DESC",
                        Listing.class)
                .setParameter("carId", carId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean hasListingsByOwner(final long ownerId) {
        final Long count = (Long) em.createQuery(
                        "SELECT COUNT(l) FROM Listing l WHERE l.car.owner.id = :ownerId")
                .setParameter("ownerId", ownerId)
                .getSingleResult();
        return count != null && count > 0;
    }

    @Override
    public HomeListingCards getHomeListingCards(
            final int limit,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("homeLimit", limit);
        appendPublicBrowseFilterParams(params, browseWallDate, excludeOwnerUserId);

        final String cheapestSelect = "SELECT CAST('C' AS VARCHAR(1)) AS home_section, l.id AS listing_id, c.brand, c.model, l.day_price, "
                + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id, l.rating_avg, l.status AS listing_status, "
                + buildReviewCountSql() + " AS review_count "
                + "FROM listings l JOIN cars c ON c.id = l.car_id WHERE l.status = 'active' "
                + publicBrowseAvailabilitySql(browseWallDate)
                + publicBrowseExcludeOwnerSql(excludeOwnerUserId)
                + "ORDER BY l.day_price ASC LIMIT :homeLimit";
        final String recentSelect = "SELECT CAST('R' AS VARCHAR(1)) AS home_section, l.id AS listing_id, c.brand, c.model, l.day_price, "
                + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id, l.rating_avg, l.status AS listing_status, "
                + buildReviewCountSql() + " AS review_count "
                + "FROM listings l JOIN cars c ON c.id = l.car_id WHERE l.status = 'active' "
                + publicBrowseAvailabilitySql(browseWallDate)
                + publicBrowseExcludeOwnerSql(excludeOwnerUserId)
                + "ORDER BY l.created_at DESC LIMIT :homeLimit";
        final String sql = "(" + cheapestSelect + ") UNION ALL (" + recentSelect + ")";

        @SuppressWarnings("unchecked")
        final List<Object[]> rows = bindParams(em.createNativeQuery(sql), params).getResultList();
        final List<ListingCard> cheapest = new ArrayList<>();
        final List<ListingCard> mostRecent = new ArrayList<>();
        for (final Object[] row : rows) {
            final ListingCard card = mapListingCard(row, 1);
            final String section = String.valueOf(row[0]);
            if ("C".equals(section)) {
                cheapest.add(card);
            } else {
                mostRecent.add(card);
            }
        }
        return new HomeListingCards(List.copyOf(cheapest), List.copyOf(mostRecent));
    }

    @Override
    public Page<ListingCard> searchListingCards(final ListingSearchCriteria criteria) {
        final DualLayerPageWindow w = DualLayerPageWindow.compute(
                criteria.getPage(), criteria.getUiPageSize(), criteria.getDbFetchSize());
        if (criteria.hasAvailabilityRange()) {
            final Map<String, Object> params = new HashMap<>();
            final StringBuilder sql = new StringBuilder(listingCardSearchSelectClause());
            appendListingCardSearchFromWhere(sql, params, criteria);
            sql.append("ORDER BY ").append(buildOrderBy(criteria.getSortBy(), criteria.getSortDirection()));
            List<ListingCard> result = runListingCardNativeQuery(sql.toString(), params);
            result = filterByAvailabilityCoverage(criteria, result, ListingCard::getListingId);
            final long total = result.size();
            final List<ListingCard> slice = DualLayerPageWindow.sliceGlobalOrdered(result, w);
            return new Page<>(slice, w.uiPage(), w.uiPageSize(), total);
        }

        final Map<String, Object> countParams = new HashMap<>();
        final StringBuilder countFromWhere = new StringBuilder();
        appendListingCardSearchFromWhere(countFromWhere, countParams, criteria);
        final Number totalObj = (Number) bindParams(
                em.createNativeQuery("SELECT COUNT(*) " + countFromWhere), countParams).getSingleResult();
        final long total = totalObj != null ? totalObj.longValue() : 0L;

        final Map<String, Object> listParams = new HashMap<>(countParams);
        listParams.put("limit", w.sqlLimit());
        listParams.put("offset", w.sqlOffset());
        final StringBuilder listSql = new StringBuilder(listingCardSearchSelectClause());
        appendListingCardSearchFromWhere(listSql, listParams, criteria);
        listSql.append("ORDER BY ")
                .append(buildOrderBy(criteria.getSortBy(), criteria.getSortDirection()))
                .append(" LIMIT :limit OFFSET :offset");
        final List<ListingCard> batch = runListingCardNativeQuery(listSql.toString(), listParams);
        final List<ListingCard> slice = DualLayerPageWindow.sliceBatch(batch, w);
        return new Page<>(slice, w.uiPage(), w.uiPageSize(), total);
    }

    @Override
    public List<ListingCard> findSimilarListingCards(
            final long listingId,
            final int limit,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("listingId", listingId);
        params.put("similarLimit", limit);
        appendPublicBrowseFilterParams(params, browseWallDate, excludeOwnerUserId);
        final String sql = "SELECT l.id AS listing_id, c.brand, c.model, l.day_price, "
                + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id, l.rating_avg, l.status AS listing_status, "
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
        return runListingCardNativeQuery(sql, params);
    }

    @Override
    public long countBrowseEligibleActiveListings(final LocalDate browseWallDate, final Long excludeOwnerUserId) {
        if (browseWallDate == null && excludeOwnerUserId == null) {
            final Long count = (Long) em.createQuery(
                            "SELECT COUNT(l) FROM Listing l WHERE l.status = :status")
                    .setParameter("status", Listing.Status.ACTIVE)
                    .getSingleResult();
            return count != null ? count : 0L;
        }
        final Map<String, Object> params = new HashMap<>();
        appendPublicBrowseFilterParams(params, browseWallDate, excludeOwnerUserId);
        final String sql = "SELECT COUNT(DISTINCT l.id) FROM listings l INNER JOIN cars c ON c.id = l.car_id "
                + "WHERE l.status = 'active' "
                + publicBrowseAvailabilitySql(browseWallDate)
                + publicBrowseExcludeOwnerSql(excludeOwnerUserId);
        final Number count = (Number) bindParams(em.createNativeQuery(sql), params).getSingleResult();
        return count != null ? count.longValue() : 0L;
    }

    // ---- SQL helpers ----

    private static String listingCardSelectPrefix() {
        return "SELECT l.id AS listing_id, c.brand, c.model, l.day_price, "
                + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id, l.rating_avg, l.status AS listing_status, "
                + buildReviewCountSql() + " AS review_count ";
    }

    private static String listingCardSearchSelectClause() {
        return "SELECT l.id AS listing_id, c.brand, c.model, l.day_price, "
                + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id, l.rating_avg, l.status AS listing_status, "
                + buildReviewCountSql() + " AS review_count ";
    }

    private static String buildReviewCountSql() {
        return "(SELECT COUNT(*) FROM reservations r "
                + "INNER JOIN reviews rv ON r.id = rv.reservation_id AND rv.rating IS NOT NULL "
                + "WHERE r.listing_id = l.id)";
    }

    private static void appendListingCardSearchFromWhere(
            final StringBuilder sql,
            final Map<String, Object> params,
            final ListingSearchCriteria criteria) {
        sql.append("FROM listings l INNER JOIN cars c ON l.car_id = c.id WHERE l.status = 'active' ");
        appendSearchFilters(sql, params, criteria);
    }

    private static void appendSearchFilters(
            final StringBuilder sql,
            final Map<String, Object> params,
            final ListingSearchCriteria criteria) {
        if (criteria.getQuery() != null) {
            final String q = "%" + escapeLike(criteria.getQuery()) + "%";
            sql.append("AND (LOWER(c.brand) LIKE LOWER(:search) ESCAPE '\\' OR LOWER(c.model) LIKE LOWER(:search) ESCAPE '\\' ")
                    .append("OR LOWER(l.title) LIKE LOWER(:search) ESCAPE '\\' OR LOWER(l.description) LIKE LOWER(:search) ESCAPE '\\' ")
                    .append("OR LOWER(CONCAT(COALESCE(l.start_point_street, ''), ' ', COALESCE(l.start_point_number, ''))) "
                            + "LIKE LOWER(:search) ESCAPE '\\') ");
            params.put("search", q);
        }
        if (!criteria.getTransmissions().isEmpty()) {
            sql.append("AND c.transmission IN (:transmissions) ");
            params.put("transmissions", criteria.getTransmissions());
        }
        if (!criteria.getPowertrains().isEmpty()) {
            sql.append("AND c.powertrain IN (:powertrains) ");
            params.put("powertrains", criteria.getPowertrains());
        }
        if (!criteria.getNeighborhoodIds().isEmpty()) {
            sql.append("AND l.neighborhood_id IN (:searchNeighborhoodIds) ");
            params.put("searchNeighborhoodIds", criteria.getNeighborhoodIds());
        }
        if (!criteria.getCarTypes().isEmpty()) {
            sql.append("AND c.type IN (:carTypes) ");
            params.put("carTypes", criteria.getCarTypes());
        }
        if (criteria.getMinPrice() != null) {
            sql.append("AND l.day_price >= :minPrice ");
            params.put("minPrice", criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() != null) {
            sql.append("AND l.day_price <= :maxPrice ");
            params.put("maxPrice", criteria.getMaxPrice());
        }
        appendRatingBandFilter(sql, criteria.getRatingBands());
        if (criteria.hasAvailabilityRange()) {
            sql.append("AND NOT EXISTS (")
                    .append("SELECT 1 FROM reservations r WHERE r.listing_id = l.id ")
                    .append("AND r.status IN ('pending', 'accepted', 'started') ")
                    .append("AND r.start_date < :resWindowEnd AND r.end_date > :resWindowStart) ");
            params.put("resWindowEnd", Timestamp.from(criteria.getAvailabilityRangeEndExclusive()));
            params.put("resWindowStart", Timestamp.from(criteria.getAvailabilityRangeStart()));
        }
        appendPublicBrowseFilterParams(params, criteria.getBrowseWallDate(), criteria.getExcludeOwnerUserId());
        sql.append(publicBrowseAvailabilitySql(criteria.getBrowseWallDate()));
        sql.append(publicBrowseExcludeOwnerSql(criteria.getExcludeOwnerUserId()));
    }

    private static void appendOwnerListingFilters(
            final StringBuilder sql,
            final Map<String, Object> params,
            final OwnerListingSearchCriteria criteria) {
        if (!criteria.getListingStatusFilters().isEmpty()) {
            sql.append("AND LOWER(l.status) IN (:ownerListingStatuses) ");
            params.put("ownerListingStatuses", criteria.getListingStatusFilters());
        }
        final String textQuery = criteria.getTextQuery();
        if (textQuery != null) {
            final String q = "%" + escapeLike(textQuery) + "%";
            sql.append("AND (LOWER(c.brand) LIKE LOWER(:ownerListingSearch) ESCAPE '\\' "
                    + "OR LOWER(c.model) LIKE LOWER(:ownerListingSearch) ESCAPE '\\' "
                    + "OR LOWER(l.title) LIKE LOWER(:ownerListingSearch) ESCAPE '\\') ");
            params.put("ownerListingSearch", q);
        }
        if (!criteria.getCarTypes().isEmpty()) {
            sql.append("AND c.type IN (:ownerCarTypes) ");
            params.put("ownerCarTypes", criteria.getCarTypes());
        }
        if (!criteria.getTransmissions().isEmpty()) {
            sql.append("AND c.transmission IN (:ownerTransmissions) ");
            params.put("ownerTransmissions", criteria.getTransmissions());
        }
        if (!criteria.getPowertrains().isEmpty()) {
            sql.append("AND c.powertrain IN (:ownerPowertrains) ");
            params.put("ownerPowertrains", criteria.getPowertrains());
        }
        if (criteria.getMinPrice() != null) {
            sql.append("AND l.day_price >= :ownerMinPrice ");
            params.put("ownerMinPrice", criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() != null) {
            sql.append("AND l.day_price <= :ownerMaxPrice ");
            params.put("ownerMaxPrice", criteria.getMaxPrice());
        }
        if (criteria.getExcludeListingId() != null) {
            sql.append("AND l.id <> :ownerExcludeListingId ");
            params.put("ownerExcludeListingId", criteria.getExcludeListingId());
        }
        appendRatingBandFilter(sql, criteria.getRatingBands());
    }

    private static void appendPublicBrowseFilterParams(
            final Map<String, Object> params,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId) {
        if (browseWallDate != null) {
            params.put("browseWallDate", java.sql.Date.valueOf(browseWallDate));
        }
        if (excludeOwnerUserId != null) {
            params.put("excludeOwnerUserId", excludeOwnerUserId);
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

    private static String buildOrderBy(final String sortBy, final String sortDirection) {
        final String col = SORT_COLUMNS.getOrDefault(sortBy, "l.created_at");
        final String dir = "asc".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        if ("rating".equals(sortBy)) {
            return col + " " + dir + " NULLS LAST, l.created_at DESC";
        }
        return col + " " + dir;
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

    private static String escapeLike(final String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // ---- result mapping helpers ----

    @SuppressWarnings("unchecked")
    private <T> List<T> runEntityNativeQuery(final String sql, final Map<String, Object> params, final Class<T> entityClass) {
        return bindParams(em.createNativeQuery(sql, entityClass), params).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<ListingCard> runListingCardNativeQuery(final String sql, final Map<String, Object> params) {
        final List<Object[]> rows = bindParams(em.createNativeQuery(sql), params).getResultList();
        final List<ListingCard> result = new ArrayList<>(rows.size());
        for (final Object[] row : rows) {
            result.add(mapListingCard(row, 0));
        }
        return result;
    }

    private static ListingCard mapListingCard(final Object[] row, final int offset) {
        final long listingId = ((Number) row[offset]).longValue();
        final String brand = (String) row[offset + 1];
        final String model = (String) row[offset + 2];
        final BigDecimal dayPrice = (BigDecimal) row[offset + 3];
        final Object rawImageId = row[offset + 4];
        final long imageId = rawImageId == null ? 0L : ((Number) rawImageId).longValue();
        final Object rawRating = row[offset + 5];
        final BigDecimal ratingAvg = rawRating == null ? null
                : new BigDecimal(rawRating.toString()).setScale(2, RoundingMode.HALF_UP);
        final String statusStr = (String) row[offset + 6];
        final Listing.Status status = statusStr == null ? null : Listing.Status.valueOf(statusStr.toUpperCase());
        final long reviewCount = ((Number) row[offset + 7]).longValue();
        return new ListingCard(listingId, brand, model, dayPrice, imageId, ratingAvg, status, reviewCount);
    }

    // ---- availability coverage filtering (ported from JDBC version) ----

    private <T> List<T> filterByAvailabilityCoverage(
            final ListingSearchCriteria criteria,
            final List<T> rows,
            final java.util.function.ToLongFunction<T> listingIdExtractor) {
        if (!criteria.hasAvailabilityRange() || rows.isEmpty()) {
            return rows;
        }
        final LocalDate fromDay = criteria.getAvailabilityRangeStart().atZone(AvailabilityPeriod.WALL_ZONE).toLocalDate();
        final LocalDate untilDay = criteria.getAvailabilityRangeEndExclusive().minusNanos(1)
                .atZone(AvailabilityPeriod.WALL_ZONE).toLocalDate();
        final List<Long> listingIds = rows.stream()
                .mapToLong(listingIdExtractor)
                .distinct()
                .boxed()
                .collect(Collectors.toList());
        final Map<Long, List<DateRange>> rangesByListingId = loadAvailabilityRanges(listingIds, fromDay, untilDay);
        return rows.stream()
                .filter(row -> coversEveryDay(rangesByListingId.get(listingIdExtractor.applyAsLong(row)), fromDay, untilDay))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Map<Long, List<DateRange>> loadAvailabilityRanges(
            final List<Long> listingIds, final LocalDate fromDay, final LocalDate untilDay) {
        if (listingIds.isEmpty()) {
            return Map.of();
        }
        final Map<String, Object> params = new HashMap<>();
        params.put("listingIds", listingIds);
        params.put("untilDay", java.sql.Date.valueOf(untilDay));
        params.put("fromDay", java.sql.Date.valueOf(fromDay));
        final List<Object[]> rows = bindParams(em.createNativeQuery(
                        "SELECT listing_id, start_date, end_date FROM listing_availability "
                                + "WHERE listing_id IN (:listingIds) "
                                + "AND start_date <= :untilDay "
                                + "AND end_date >= :fromDay "
                                + "ORDER BY listing_id ASC, start_date ASC, end_date ASC"),
                params)
                .getResultList();
        final Map<Long, List<DateRange>> result = new HashMap<>();
        for (final Object[] row : rows) {
            final long lid = ((Number) row[0]).longValue();
            final LocalDate start = toLocalDate(row[1]);
            final LocalDate end = toLocalDate(row[2]);
            result.computeIfAbsent(lid, ignored -> new ArrayList<>()).add(new DateRange(start, end));
        }
        return result;
    }

    private static LocalDate toLocalDate(final Object o) {
        if (o instanceof LocalDate) {
            return (LocalDate) o;
        }
        if (o instanceof java.sql.Date) {
            return ((java.sql.Date) o).toLocalDate();
        }
        return LocalDate.parse(o.toString());
    }

    private static boolean coversEveryDay(final List<DateRange> ranges, final LocalDate fromDay, final LocalDate untilDay) {
        if (ranges == null || ranges.isEmpty()) {
            return false;
        }
        LocalDate nextDayToCover = fromDay;
        for (final DateRange range : ranges) {
            if (range.end.isBefore(nextDayToCover)) {
                continue;
            }
            if (range.start.isAfter(nextDayToCover)) {
                return false;
            }
            if (!range.end.isBefore(untilDay)) {
                return true;
            }
            nextDayToCover = range.end.plusDays(1);
        }
        return nextDayToCover.isAfter(untilDay);
    }

    private static final class DateRange {
        final LocalDate start;
        final LocalDate end;
        DateRange(final LocalDate start, final LocalDate end) {
            this.start = start;
            this.end = end;
        }
    }
}
