package ar.edu.itba.paw.persistence.hibernate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
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
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.Review;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.HomeListingCards;
import ar.edu.itba.paw.models.dto.ListingCard;
import ar.edu.itba.paw.models.dto.ListingDetail;
import ar.edu.itba.paw.models.dto.ListingPriceMarketInsight;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.pagination.DualLayerPageWindow;
import ar.edu.itba.paw.models.util.ListingSearchCriteria;
import ar.edu.itba.paw.models.util.OwnerListingSearchCriteria;
import ar.edu.itba.paw.persistence.ListingDao;

@Transactional(readOnly = true)
@Repository
public class ListingJpaDao implements ListingDao {

    private static final Map<String, String> NATIVE_SORT_COLUMNS = Map.of(
            "price",  "l.day_price",
            "date",   "l.created_at",
            "rating", "l.rating_avg"
    );

    private static final Map<String, String> JPQL_SORT_PROPERTIES = Map.of(
            "price",  "l.dayPrice",
            "date",   "l.createdAt",
            "rating", "l.ratingAvg"
    );

    /** Public browse: only cars whose catalog brand and model are validated. */
    private static final String JPQL_VALIDATED_CAR_MODEL =
            " AND l.car.carModel IS NOT NULL"
            + " AND l.car.carModel.validated = TRUE"
            + " AND l.car.carModel.brand.validated = TRUE";

    /**
     * Native JOIN used only in paginated ID queries (1+1 pattern, step 1).
     * Filters to validated catalog entries for public-facing listings.
     */
    private static final String VALIDATED_MODEL_JOIN =
            "INNER JOIN car_models cm ON cm.id = c.model_id AND cm.validated = TRUE "
            + "INNER JOIN car_brands cb ON cb.id = cm.brand_id AND cb.validated = TRUE ";

    /** Native LEFT JOIN for owner listing ID pagination (legacy brand/model columns may still exist in DB). */
    private static final String OWNER_MODEL_JOIN =
            "LEFT JOIN car_models cm ON cm.id = c.model_id "
            + "LEFT JOIN car_brands cb ON cb.id = cm.brand_id ";

    private static final List<Reservation.Status> BLOCKING_RESERVATION_STATUSES = List.of(
            Reservation.Status.PENDING,
            Reservation.Status.ACCEPTED,
            Reservation.Status.STARTED);

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
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
    @Transactional
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
    @Transactional
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
    @Transactional
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
        final Map<String, Object> params = new HashMap<>();
        params.put("activeStatus", Listing.Status.ACTIVE);
        final StringBuilder jpql = new StringBuilder(
                "SELECT DISTINCT l FROM Listing l WHERE l.status = :activeStatus");
        jpql.append(JPQL_VALIDATED_CAR_MODEL);
        appendJpqlSearchFilters(jpql, params, criteria);
        jpql.append(" ORDER BY l.createdAt DESC");
        @SuppressWarnings("unchecked")
        final List<Listing> result = bindParams(em.createQuery(jpql.toString(), Listing.class), params)
                .getResultList();
        return filterByAvailabilityCoverage(criteria, result, Listing::getId);
    }

    @Override
    public List<Listing> getCheapestListings(final int limit) {
        return em.createQuery(
                        "FROM Listing l WHERE l.status = :status"
                        + " AND l.car.carModel IS NOT NULL"
                        + " AND l.car.carModel.validated = TRUE"
                        + " AND l.car.carModel.brand.validated = TRUE"
                        + " ORDER BY l.dayPrice ASC",
                        Listing.class)
                .setParameter("status", Listing.Status.ACTIVE)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public List<Listing> getMostRecentListings(final int limit) {
        return em.createQuery(
                        "FROM Listing l WHERE l.status = :status"
                        + " AND l.car.carModel IS NOT NULL"
                        + " AND l.car.carModel.validated = TRUE"
                        + " AND l.car.carModel.brand.validated = TRUE"
                        + " ORDER BY l.createdAt DESC",
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
        return listingCardsFromOrderedIds(loadPublicListingIdsPage(
                "l.day_price ASC",
                offset,
                limit,
                browseWallDate,
                excludeOwnerUserId,
                null));
    }

    @Override
    public List<ListingCard> getMostRecentListingCardsWindow(
            final int offset,
            final int limit,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId) {
        return listingCardsFromOrderedIds(loadPublicListingIdsPage(
                "l.created_at DESC",
                offset,
                limit,
                browseWallDate,
                excludeOwnerUserId,
                null));
    }

    @Override
    public Page<ListingCard> getOwnerListingCards(final OwnerListingSearchCriteria criteria) {
        final int page = criteria.getPage();
        final int pageSize = criteria.getPageSize();

        final Map<String, Object> countParams = new HashMap<>();
        countParams.put("ownerId", criteria.getOwnerId());
        final StringBuilder countJpql = new StringBuilder(
                "SELECT COUNT(l) FROM Listing l WHERE l.car.owner.id = :ownerId");
        appendJpqlOwnerListingFilters(countJpql, countParams, criteria);
        final Long total = (Long) bindParams(em.createQuery(countJpql.toString(), Long.class), countParams)
                .getSingleResult();

        final int offset = page * pageSize;
        final Map<String, Object> idParams = new HashMap<>();
        idParams.put("ownerId", criteria.getOwnerId());
        idParams.put("limit", pageSize);
        idParams.put("offset", offset);
        final StringBuilder idSql = new StringBuilder(
                "SELECT l.id FROM listings l JOIN cars c ON c.id = l.car_id "
                + OWNER_MODEL_JOIN
                + "WHERE c.owner_id = :ownerId ");
        appendNativeOwnerListingFilters(idSql, idParams, criteria);
        idSql.append("ORDER BY ")
                .append(buildNativeOrderBy(criteria.getSortBy(), criteria.getSortDirection()))
                .append(" LIMIT :limit OFFSET :offset");
        final List<ListingCard> content = listingCardsFromOrderedIds(runListingIdNativeQuery(idSql.toString(), idParams));
        return new Page<>(content, page, pageSize, total != null ? total : 0L);
    }

    @Override
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
        final List<ListingCard> cheapest = loadPublicListingCardsByJpql(
                "l.dayPrice ASC", limit, browseWallDate, excludeOwnerUserId);
        final List<ListingCard> mostRecent = loadPublicListingCardsByJpql(
                "l.createdAt DESC", limit, browseWallDate, excludeOwnerUserId);
        return new HomeListingCards(List.copyOf(cheapest), List.copyOf(mostRecent));
    }

    @Override
    public Page<ListingCard> searchListingCards(final ListingSearchCriteria criteria) {
        final DualLayerPageWindow w = DualLayerPageWindow.compute(
                criteria.getPage(), criteria.getUiPageSize(), criteria.getDbFetchSize());
        if (criteria.hasAvailabilityRange()) {
            final Map<String, Object> params = new HashMap<>();
            params.put("activeStatus", Listing.Status.ACTIVE);
            final StringBuilder jpql = new StringBuilder(
                    "SELECT DISTINCT l.id FROM Listing l WHERE l.status = :activeStatus");
            jpql.append(JPQL_VALIDATED_CAR_MODEL);
            appendJpqlSearchFilters(jpql, params, criteria);
            jpql.append(" ORDER BY ").append(buildJpqlOrderBy(criteria.getSortBy(), criteria.getSortDirection()));
            @SuppressWarnings("unchecked")
            final List<Long> listingIds = bindParams(em.createQuery(jpql.toString(), Long.class), params)
                    .getResultList();
            List<ListingCard> result = listingCardsFromOrderedIds(listingIds);
            result = filterByAvailabilityCoverage(criteria, result, ListingCard::getListingId);
            final long total = result.size();
            final List<ListingCard> slice = DualLayerPageWindow.sliceGlobalOrdered(result, w);
            return new Page<>(slice, w.uiPage(), w.uiPageSize(), total);
        }

        final Map<String, Object> countParams = new HashMap<>();
        countParams.put("activeStatus", Listing.Status.ACTIVE);
        final StringBuilder countJpql = new StringBuilder(
                "SELECT COUNT(DISTINCT l) FROM Listing l WHERE l.status = :activeStatus");
        countJpql.append(JPQL_VALIDATED_CAR_MODEL);
        appendJpqlSearchFilters(countJpql, countParams, criteria);
        final Long totalObj = (Long) bindParams(em.createQuery(countJpql.toString(), Long.class), countParams)
                .getSingleResult();
        final long total = totalObj != null ? totalObj : 0L;

        final List<Long> pageIds = loadPublicListingIdsPage(
                buildNativeOrderBy(criteria.getSortBy(), criteria.getSortDirection()),
                w.sqlOffset(),
                w.sqlLimit(),
                criteria.getBrowseWallDate(),
                criteria.getExcludeOwnerUserId(),
                criteria);
        final List<ListingCard> batch = listingCardsFromOrderedIds(pageIds);
        final List<ListingCard> slice = DualLayerPageWindow.sliceBatch(batch, w);
        return new Page<>(slice, w.uiPage(), w.uiPageSize(), total);
    }

    @Override
    public List<ListingCard> findSimilarListingCards(
            final long listingId,
            final int limit,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId) {
        final Optional<Listing> anchor = getListingById(listingId);
        if (anchor.isEmpty()) {
            return List.of();
        }
        final Car refCar = anchor.get().getCar();
        final Map<String, Object> params = new HashMap<>();
        params.put("activeStatus", Listing.Status.ACTIVE);
        params.put("listingId", listingId);
        params.put("refType", refCar.getType());
        params.put("refPowertrain", refCar.getPowertrain());
        params.put("refTransmission", refCar.getTransmission());
        if (browseWallDate != null) {
            params.put("browseWallDate", browseWallDate);
        }
        if (excludeOwnerUserId != null) {
            params.put("excludeOwnerUserId", excludeOwnerUserId);
        }
        final StringBuilder jpql = new StringBuilder(
                "SELECT l.id FROM Listing l WHERE l.status = :activeStatus "
                + "AND l.id <> :listingId "
                + "AND l.car.type = :refType AND l.car.powertrain = :refPowertrain "
                + "AND l.car.transmission = :refTransmission");
        jpql.append(JPQL_VALIDATED_CAR_MODEL);
        appendJpqlPublicBrowseFilters(jpql, browseWallDate, excludeOwnerUserId);
        jpql.append(" ORDER BY l.createdAt DESC");
        @SuppressWarnings("unchecked")
        final List<Long> ids = bindParams(em.createQuery(jpql.toString(), Long.class), params)
                .setMaxResults(limit)
                .getResultList();
        return listingCardsFromOrderedIds(ids);
    }

    @Override
    public Optional<ListingPriceMarketInsight> findActiveDayPriceMarketInsightByBrandAndModel(
            final String brand,
            final String model,
            final Long excludeListingId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("brand", brand);
        params.put("model", model);
        final StringBuilder sql = new StringBuilder(
                "SELECT MIN(l.day_price), MAX(l.day_price), AVG(l.day_price), COUNT(*) "
                + "FROM listings l "
                + "JOIN cars c ON c.id = l.car_id "
                + "WHERE l.status = 'active' "
                + "AND LOWER(TRIM(c.brand)) = LOWER(TRIM(:brand)) "
                + "AND LOWER(TRIM(c.model)) = LOWER(TRIM(:model)) ");
        if (excludeListingId != null) {
            sql.append("AND l.id <> :excludeListingId ");
            params.put("excludeListingId", excludeListingId);
        }
        final Object[] row = (Object[]) bindParams(em.createNativeQuery(sql.toString()), params).getSingleResult();
        if (row == null || row[0] == null || row[1] == null || row[2] == null) {
            return Optional.empty();
        }
        final long count = ((Number) row[3]).longValue();
        if (count == 0L) {
            return Optional.empty();
        }
        final BigDecimal min = toBigDecimal(row[0]);
        final BigDecimal max = toBigDecimal(row[1]);
        final BigDecimal avg = toBigDecimal(row[2]);
        return Optional.of(new ListingPriceMarketInsight(min, max, avg, count));
    }

    private static BigDecimal toBigDecimal(final Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }

    @Override
    public long countBrowseEligibleActiveListings(final LocalDate browseWallDate, final Long excludeOwnerUserId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("activeStatus", Listing.Status.ACTIVE);
        final StringBuilder jpql = new StringBuilder(
                "SELECT COUNT(DISTINCT l) FROM Listing l WHERE l.status = :activeStatus");
        jpql.append(JPQL_VALIDATED_CAR_MODEL);
        appendJpqlPublicBrowseFilters(jpql, browseWallDate, excludeOwnerUserId);
        if (browseWallDate != null) {
            params.put("browseWallDate", browseWallDate);
        }
        if (excludeOwnerUserId != null) {
            params.put("excludeOwnerUserId", excludeOwnerUserId);
        }
        final Long count = (Long) bindParams(em.createQuery(jpql.toString(), Long.class), params).getSingleResult();
        return count != null ? count : 0L;
    }

    // ---- query helpers ----

    /** Step 1 of 1+1: native SQL returns listing IDs only (filter + sort + page). */
    private List<Long> loadPublicListingIdsPage(
            final String nativeOrderBy,
            final int offset,
            final int limit,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId,
            final ListingSearchCriteria searchCriteria) {
        final Map<String, Object> params = new HashMap<>();
        params.put("limit", limit);
        params.put("offset", offset);
        appendPublicBrowseFilterParams(params, browseWallDate, excludeOwnerUserId);
        final StringBuilder sql = new StringBuilder(
                "SELECT l.id FROM listings l INNER JOIN cars c ON l.car_id = c.id "
                + VALIDATED_MODEL_JOIN
                + "WHERE l.status = 'active' ");
        if (searchCriteria != null) {
            appendNativeSearchFilters(sql, params, searchCriteria);
        } else {
            sql.append(publicBrowseAvailabilitySql(browseWallDate));
            sql.append(publicBrowseExcludeOwnerSql(excludeOwnerUserId));
        }
        sql.append(" ORDER BY ").append(nativeOrderBy).append(" LIMIT :limit OFFSET :offset");
        return runListingIdNativeQuery(sql.toString(), params);
    }

    /** Non-paginated public browse slices (home sections): pure JPQL + setMaxResults. */
    private List<ListingCard> loadPublicListingCardsByJpql(
            final String jpqlOrderBy,
            final int limit,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("activeStatus", Listing.Status.ACTIVE);
        if (browseWallDate != null) {
            params.put("browseWallDate", browseWallDate);
        }
        if (excludeOwnerUserId != null) {
            params.put("excludeOwnerUserId", excludeOwnerUserId);
        }
        final StringBuilder jpql = new StringBuilder(
                "SELECT l.id FROM Listing l WHERE l.status = :activeStatus");
        jpql.append(JPQL_VALIDATED_CAR_MODEL);
        appendJpqlPublicBrowseFilters(jpql, browseWallDate, excludeOwnerUserId);
        jpql.append(" ORDER BY ").append(jpqlOrderBy);
        @SuppressWarnings("unchecked")
        final List<Long> ids = bindParams(em.createQuery(jpql.toString(), Long.class), params)
                .setMaxResults(limit)
                .getResultList();
        return listingCardsFromOrderedIds(ids);
    }

    /** Step 2 of 1+1: JPQL loads listings and assembles {@link ListingCard}s preserving ID order. */
    private List<ListingCard> listingCardsFromOrderedIds(final List<Long> orderedListingIds) {
        if (orderedListingIds.isEmpty()) {
            return List.of();
        }
        final List<Listing> listings = loadListingsForCardsByIds(orderedListingIds);
        final Map<Long, Listing> byId = listings.stream()
                .collect(Collectors.toMap(Listing::getId, Function.identity(), (a, b) -> a));
        final List<Long> carIds = listings.stream()
                .map(l -> l.getCar().getId())
                .distinct()
                .collect(Collectors.toList());
        final Map<Long, Long> imageByCarId = loadCoverImageIdByCarIds(carIds);
        final Map<Long, Long> reviewsByListingId = loadReviewCountsByListingIds(orderedListingIds);
        return orderedListingIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(l -> toListingCard(
                        l,
                        imageByCarId.getOrDefault(l.getCar().getId(), 0L),
                        reviewsByListingId.getOrDefault(l.getId(), 0L)))
                .collect(Collectors.toList());
    }

    private List<Listing> loadListingsForCardsByIds(final List<Long> listingIds) {
        @SuppressWarnings("unchecked")
        final List<Listing> listings = bindParams(
                em.createQuery(
                        "SELECT DISTINCT l FROM Listing l "
                                + "JOIN FETCH l.car c "
                                + "LEFT JOIN FETCH c.carModel m "
                                + "LEFT JOIN FETCH m.brand "
                                + "WHERE l.id IN :ids",
                        Listing.class),
                Map.of("ids", listingIds))
                .getResultList();
        return listings;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Long> loadReviewCountsByListingIds(final Collection<Long> listingIds) {
        if (listingIds.isEmpty()) {
            return Map.of();
        }
        final List<Object[]> rows = bindParams(
                em.createQuery(
                        "SELECT r.reservation.listing.id, COUNT(r) FROM Review r "
                                + "WHERE r.reservation.listing.id IN :listingIds AND r.rating IS NOT NULL "
                                + "GROUP BY r.reservation.listing.id"),
                Map.of("listingIds", listingIds))
                .getResultList();
        final Map<Long, Long> result = new HashMap<>();
        for (final Object[] row : rows) {
            result.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return result;
    }

    private Map<Long, Long> loadCoverImageIdByCarIds(final Collection<Long> carIds) {
        if (carIds.isEmpty()) {
            return Map.of();
        }
        final List<CarPicture> pictures = bindParams(
                em.createQuery(
                        "FROM CarPicture cp WHERE cp.car.id IN :carIds ORDER BY cp.car.id ASC, cp.displayOrder ASC",
                        CarPicture.class),
                Map.of("carIds", carIds))
                .getResultList();
        final Map<Long, Long> result = new HashMap<>();
        for (final CarPicture picture : pictures) {
            result.putIfAbsent(picture.getCar().getId(), picture.getImageId());
        }
        return result;
    }

    private static ListingCard toListingCard(final Listing listing, final long imageId, final long reviewCount) {
        final Car car = listing.getCar();
        return new ListingCard(
                listing.getId(),
                car.getId(),
                car.getBrand(),
                car.getModel(),
                listing.getDayPrice(),
                imageId,
                listing.getRatingAvg().orElse(null),
                listing.getStatus(),
                reviewCount);
    }

    @SuppressWarnings("unchecked")
    private List<Long> runListingIdNativeQuery(final String sql, final Map<String, Object> params) {
        final List<Number> raw = bindParams(em.createNativeQuery(sql), params).getResultList();
        return new ArrayList<>(raw.stream().map(Number::longValue).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private static void appendJpqlPublicBrowseFilters(
            final StringBuilder jpql,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId) {
        if (browseWallDate != null) {
            jpql.append(" AND EXISTS (SELECT 1 FROM ListingAvailability la "
                    + "WHERE la.listing.id = l.id AND la.endInclusive >= :browseWallDate)");
        }
        if (excludeOwnerUserId != null) {
            jpql.append(" AND l.car.owner.id <> :excludeOwnerUserId");
        }
    }

    private static void appendJpqlSearchFilters(
            final StringBuilder jpql,
            final Map<String, Object> params,
            final ListingSearchCriteria criteria) {
        if (criteria.getQuery() != null) {
            final String q = "%" + escapeLike(criteria.getQuery()) + "%";
            jpql.append(" AND (LOWER(l.car.carModel.brand.name) LIKE LOWER(:search) ESCAPE '\\'"
                    + " OR LOWER(l.car.carModel.name) LIKE LOWER(:search) ESCAPE '\\'"
                    + " OR LOWER(l.title) LIKE LOWER(:search) ESCAPE '\\'"
                    + " OR LOWER(l.description) LIKE LOWER(:search) ESCAPE '\\'"
                    + " OR LOWER(CONCAT(COALESCE(l.startPointStreet, ''), ' ', COALESCE(l.startPointNumber, '')))"
                    + " LIKE LOWER(:search) ESCAPE '\\')");
            params.put("search", q);
        }
        if (!criteria.getTransmissions().isEmpty()) {
            jpql.append(" AND l.car.transmission IN :transmissions");
            params.put("transmissions", criteria.getTransmissions());
        }
        if (!criteria.getPowertrains().isEmpty()) {
            jpql.append(" AND l.car.powertrain IN :powertrains");
            params.put("powertrains", criteria.getPowertrains());
        }
        if (!criteria.getNeighborhoodIds().isEmpty()) {
            jpql.append(" AND l.neighborhood.id IN :searchNeighborhoodIds");
            params.put("searchNeighborhoodIds", criteria.getNeighborhoodIds());
        }
        if (!criteria.getCarTypes().isEmpty()) {
            jpql.append(" AND l.car.type IN :carTypes");
            params.put("carTypes", criteria.getCarTypes());
        }
        if (criteria.getMinPrice() != null) {
            jpql.append(" AND l.dayPrice >= :minPrice");
            params.put("minPrice", criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() != null) {
            jpql.append(" AND l.dayPrice <= :maxPrice");
            params.put("maxPrice", criteria.getMaxPrice());
        }
        appendJpqlRatingBandFilter(jpql, criteria.getRatingBands());
        if (criteria.hasAvailabilityRange()) {
            jpql.append(" AND NOT EXISTS (SELECT 1 FROM Reservation r WHERE r.listing.id = l.id"
                    + " AND r.status IN :blockingReservationStatuses"
                    + " AND r.startDate < :resWindowEnd AND r.endDate > :resWindowStart)");
            params.put("blockingReservationStatuses", BLOCKING_RESERVATION_STATUSES);
            params.put("resWindowEnd", criteria.getAvailabilityRangeEndExclusive());
            params.put("resWindowStart", criteria.getAvailabilityRangeStart());
        }
        appendJpqlPublicBrowseFilters(jpql, criteria.getBrowseWallDate(), criteria.getExcludeOwnerUserId());
        if (criteria.getBrowseWallDate() != null) {
            params.put("browseWallDate", criteria.getBrowseWallDate());
        }
        if (criteria.getExcludeOwnerUserId() != null) {
            params.put("excludeOwnerUserId", criteria.getExcludeOwnerUserId());
        }
    }

    private static void appendNativeSearchFilters(
            final StringBuilder sql,
            final Map<String, Object> params,
            final ListingSearchCriteria criteria) {
        if (criteria.getQuery() != null) {
            final String q = "%" + escapeLike(criteria.getQuery()) + "%";
            sql.append("AND (LOWER(cb.name) LIKE LOWER(:search) ESCAPE '\\' OR LOWER(cm.name) LIKE LOWER(:search) ESCAPE '\\' ")
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
        appendNativeRatingBandFilter(sql, criteria.getRatingBands());
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

    private static void appendJpqlOwnerListingFilters(
            final StringBuilder jpql,
            final Map<String, Object> params,
            final OwnerListingSearchCriteria criteria) {
        if (!criteria.getListingStatusFilters().isEmpty()) {
            jpql.append(" AND l.status IN :ownerListingStatuses");
            params.put("ownerListingStatuses", criteria.getListingStatusFilters().stream()
                    .map(s -> Listing.Status.valueOf(s.toUpperCase()))
                    .collect(Collectors.toList()));
        }
        final String textQuery = criteria.getTextQuery();
        if (textQuery != null) {
            final String q = "%" + escapeLike(textQuery) + "%";
            jpql.append(" AND (LOWER(l.car.carModel.brand.name) LIKE LOWER(:ownerListingSearch) ESCAPE '\\'"
                    + " OR LOWER(l.car.carModel.name) LIKE LOWER(:ownerListingSearch) ESCAPE '\\'"
                    + " OR LOWER(l.title) LIKE LOWER(:ownerListingSearch) ESCAPE '\\')");
            params.put("ownerListingSearch", q);
        }
        if (!criteria.getCarTypes().isEmpty()) {
            jpql.append(" AND l.car.type IN :ownerCarTypes");
            params.put("ownerCarTypes", criteria.getCarTypes());
        }
        if (!criteria.getTransmissions().isEmpty()) {
            jpql.append(" AND l.car.transmission IN :ownerTransmissions");
            params.put("ownerTransmissions", criteria.getTransmissions());
        }
        if (!criteria.getPowertrains().isEmpty()) {
            jpql.append(" AND l.car.powertrain IN :ownerPowertrains");
            params.put("ownerPowertrains", criteria.getPowertrains());
        }
        if (criteria.getMinPrice() != null) {
            jpql.append(" AND l.dayPrice >= :ownerMinPrice");
            params.put("ownerMinPrice", criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() != null) {
            jpql.append(" AND l.dayPrice <= :ownerMaxPrice");
            params.put("ownerMaxPrice", criteria.getMaxPrice());
        }
        if (criteria.getExcludeListingId() != null) {
            jpql.append(" AND l.id <> :ownerExcludeListingId");
            params.put("ownerExcludeListingId", criteria.getExcludeListingId());
        }
        appendJpqlRatingBandFilter(jpql, criteria.getRatingBands());
    }

    private static void appendNativeOwnerListingFilters(
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
            sql.append("AND (LOWER(COALESCE(cb.name, c.brand, '')) LIKE LOWER(:ownerListingSearch) ESCAPE '\\' "
                    + "OR LOWER(COALESCE(cm.name, c.model, '')) LIKE LOWER(:ownerListingSearch) ESCAPE '\\' "
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
        appendNativeRatingBandFilter(sql, criteria.getRatingBands());
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

    private static String buildNativeOrderBy(final String sortBy, final String sortDirection) {
        final String col = NATIVE_SORT_COLUMNS.getOrDefault(sortBy, "l.created_at");
        final String dir = "asc".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        if ("rating".equals(sortBy)) {
            return col + " " + dir + " NULLS LAST, l.created_at DESC";
        }
        return col + " " + dir;
    }

    private static String buildJpqlOrderBy(final String sortBy, final String sortDirection) {
        final String prop = JPQL_SORT_PROPERTIES.getOrDefault(sortBy, "l.createdAt");
        final String dir = "asc".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        if ("rating".equals(sortBy)) {
            return prop + " " + dir + " NULLS LAST, l.createdAt DESC";
        }
        return prop + " " + dir;
    }

    private static void appendNativeRatingBandFilter(final StringBuilder sql, final List<String> ratingBands) {
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

    private static void appendJpqlRatingBandFilter(final StringBuilder jpql, final List<String> ratingBands) {
        if (ratingBands.isEmpty()) {
            return;
        }
        final List<String> conditions = new ArrayList<>();
        if (ratingBands.contains("UNDER_2")) {
            conditions.add("l.ratingAvg < 2");
        }
        if (ratingBands.contains("2_TO_3")) {
            conditions.add("(l.ratingAvg >= 2 AND l.ratingAvg < 3)");
        }
        if (ratingBands.contains("3_TO_4")) {
            conditions.add("(l.ratingAvg >= 3 AND l.ratingAvg < 4)");
        }
        if (ratingBands.contains("OVER_4")) {
            conditions.add("l.ratingAvg >= 4");
        }
        if (!conditions.isEmpty()) {
            jpql.append(" AND (").append(String.join(" OR ", conditions)).append(")");
        }
    }

    private static String escapeLike(final String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // ---- availability coverage filtering ----

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

    private Map<Long, List<DateRange>> loadAvailabilityRanges(
            final List<Long> listingIds, final LocalDate fromDay, final LocalDate untilDay) {
        if (listingIds.isEmpty()) {
            return Map.of();
        }
        final List<ListingAvailability> rows = bindParams(
                em.createQuery(
                        "FROM ListingAvailability la WHERE la.listing.id IN :listingIds "
                                + "AND la.startInclusive <= :untilDay "
                                + "AND la.endInclusive >= :fromDay "
                                + "ORDER BY la.listing.id ASC, la.startInclusive ASC, la.endInclusive ASC",
                        ListingAvailability.class),
                Map.of(
                        "listingIds", listingIds,
                        "untilDay", untilDay,
                        "fromDay", fromDay))
                .getResultList();
        final Map<Long, List<DateRange>> result = new HashMap<>();
        for (final ListingAvailability availability : rows) {
            result.computeIfAbsent(availability.getListing().getId(), ignored -> new ArrayList<>())
                    .add(new DateRange(availability.getStartInclusive(), availability.getEndInclusive()));
        }
        return result;
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
