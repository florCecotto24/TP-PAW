package ar.edu.itba.paw.persistence.hibernate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarModel;
import ar.edu.itba.paw.models.domain.CarPicture;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.util.OwnerListingSearchCriteria;
import ar.edu.itba.paw.persistence.CarDao;
import static ar.edu.itba.paw.persistence.util.JpaQueryUtils.bindParams;

@Transactional(readOnly = true)
@Repository
public class CarJpaDao implements CarDao {

    @PersistenceContext
    private EntityManager em;

    private final int carCatalogLimit;

    public CarJpaDao(@Value("${app.listing.car-catalog-limit:8}") final int carCatalogLimit) {
        this.carCatalogLimit = Math.max(1, carCatalogLimit);
    }

    @Override
    @Transactional
    public Car createCar(final long ownerId, final String plate, final long carModelId,
                         final Car.Type type, final Car.Powertrain powertrain,
                         final Car.Transmission transmission) {
        final User ownerRef = em.getReference(User.class, ownerId);
        // Load carModel with JOIN FETCH so brand is available outside the transaction (JSP display)
        final CarModel carModel = em.createQuery(
                        "FROM CarModel m JOIN FETCH m.brand WHERE m.id = :id", CarModel.class)
                .setParameter("id", carModelId)
                .getSingleResult();
        final OffsetDateTime now = OffsetDateTime.now();
        final Car car = Car.builder()
                .owner(ownerRef)
                .plate(plate)
                .type(type)
                .powertrain(powertrain)
                .transmission(transmission)
                .status(Car.Status.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        car.setCarModel(carModel);
        em.persist(car);
        return car;
    }

    @Override
    public boolean existsByOwnerAndPlate(final long ownerId, final String plate) {
        final Long count = (Long) em.createQuery(
                        "SELECT COUNT(c) FROM Car c WHERE c.owner.id = :ownerId AND c.plate = :plate")
                .setParameter("ownerId", ownerId)
                .setParameter("plate", plate)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public Optional<Car> getCarById(final long id) {
        return em.createQuery(
                        "FROM Car c LEFT JOIN FETCH c.carModel m LEFT JOIN FETCH m.brand WHERE c.id = :id",
                        Car.class)
                .setParameter("id", id)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public List<Car> getCheapestCars() {
        return loadCarsByOrderedIds(loadActiveCarCatalogOrderedNativeIds("l.day_price ASC"));
    }

    @Override
    public List<Car> getMostRecentCars() {
        return loadCarsByOrderedIds(loadActiveCarCatalogOrderedNativeIds("l.created_at DESC"));
    }

    private List<Long> loadActiveCarCatalogOrderedNativeIds(final String orderBySql) {
        final String sql = "SELECT c.id FROM cars c "
                + "INNER JOIN listings l ON l.car_id = c.id "
                + "INNER JOIN car_models cm ON cm.id = c.model_id AND cm.validated = TRUE "
                + "INNER JOIN car_brands cb ON cb.id = cm.brand_id AND cb.validated = TRUE "
                + "WHERE c.status = 'active' "
                + "ORDER BY " + orderBySql;
        @SuppressWarnings("unchecked")
        final List<Number> raw =
                em.createNativeQuery(sql).setMaxResults(carCatalogLimit).getResultList();
        return new ArrayList<>(raw.stream().map(Number::longValue).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private List<Car> loadCarsByOrderedIds(final List<Long> orderedCarIds) {
        if (orderedCarIds.isEmpty()) {
            return List.of();
        }
        final List<Car> cars =
                bindParams(em.createQuery(
                        "FROM Car c LEFT JOIN FETCH c.carModel m LEFT JOIN FETCH m.brand WHERE c.id IN :ids",
                        Car.class), Map.of("ids", orderedCarIds)).getResultList();
        final Map<Long, Car> byId =
                cars.stream().collect(Collectors.toMap(Car::getId, Function.identity(), (a, b) -> a));
        return orderedCarIds.stream().map(byId::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Owner car cards — 1+1 pattern
    // -------------------------------------------------------------------------

    @Override
    public Page<CarCard> getOwnerCarCards(final OwnerListingSearchCriteria criteria) {
        final int page  = criteria.getPage();
        final int pageSize = criteria.getPageSize();

        // Step 1: COUNT via JPQL (no native SQL)
        final Map<String, Object> countParams = new HashMap<>();
        countParams.put("ownerId", criteria.getOwnerId());
        final StringBuilder countJpql = new StringBuilder(
                "SELECT COUNT(DISTINCT c.id) FROM Car c "
                + "LEFT JOIN c.listings l "
                + "WHERE c.owner.id = :ownerId "
                + "AND (l IS NULL OR l.id = (SELECT MAX(l2.id) FROM Listing l2 WHERE l2.car = c)) ");
        appendOwnerCarJpqlFilters(countJpql, countParams, criteria);
        final long total = (Long) bindParams(em.createQuery(countJpql.toString()), countParams).getSingleResult();

        // Step 2: paginated car IDs via native SQL (IDs only — listing JOIN only for ordering/price)
        final int offset = page * pageSize;
        final Map<String, Object> idsParams = new HashMap<>();
        idsParams.put("ownerId", criteria.getOwnerId());
        idsParams.put("limit", pageSize);
        idsParams.put("offset", offset);
        final StringBuilder idsSql = new StringBuilder(
                "SELECT c.id FROM cars c "
                + "LEFT JOIN listings l ON l.id = (SELECT MAX(id) FROM listings WHERE car_id = c.id) "
                + "LEFT JOIN car_models cm ON cm.id = c.model_id "
                + "LEFT JOIN car_brands cb ON cb.id = cm.brand_id "
                + "WHERE c.owner_id = :ownerId ");
        appendOwnerCarNativeSqlFilters(idsSql, idsParams, criteria);
        idsSql.append("ORDER BY ").append(buildCarOrderBy(criteria.getSortBy(), criteria.getSortDirection()))
              .append(" LIMIT :limit OFFSET :offset");
        @SuppressWarnings("unchecked")
        final List<Number> rawIds = bindParams(em.createNativeQuery(idsSql.toString()), idsParams).getResultList();
        final List<Long> orderedCarIds = new ArrayList<>(rawIds.stream()
                .map(Number::longValue)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        // Steps 3-5: load entities and build cards
        final List<CarCard> content = buildCarCards(orderedCarIds);
        return new Page<>(content, page, pageSize, total);
    }

    private List<CarCard> buildCarCards(final List<Long> orderedCarIds) {
        if (orderedCarIds.isEmpty()) {
            return List.of();
        }
        final List<Car> cars = bindParams(em.createQuery(
                "FROM Car c LEFT JOIN FETCH c.carModel m LEFT JOIN FETCH m.brand WHERE c.id IN :ids",
                Car.class), Map.of("ids", orderedCarIds)).getResultList();
        final Map<Long, Car> byId = cars.stream()
                .collect(Collectors.toMap(Car::getId, Function.identity(), (a, b) -> a));

        final Map<Long, Long>    coverImages    = loadCoverImageIdByCarIds(orderedCarIds);
        final Map<Long, Listing> latestListings = loadLatestListingByCarIds(orderedCarIds);

        return orderedCarIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(car -> toCarCard(car,
                        coverImages.getOrDefault(car.getId(), 0L),
                        latestListings.get(car.getId())))
                .collect(Collectors.toList());
    }

    private Map<Long, Long> loadCoverImageIdByCarIds(final Collection<Long> carIds) {
        if (carIds.isEmpty()) {
            return Map.of();
        }
        final List<CarPicture> pictures = bindParams(em.createQuery(
                "FROM CarPicture cp WHERE cp.car.id IN :carIds ORDER BY cp.car.id ASC, cp.displayOrder ASC",
                CarPicture.class), Map.of("carIds", carIds)).getResultList();
        final Map<Long, Long> result = new HashMap<>();
        for (final CarPicture picture : pictures) {
            result.putIfAbsent(picture.getCar().getId(), picture.getImageId());
        }
        return result;
    }

    private Map<Long, Listing> loadLatestListingByCarIds(final Collection<Long> carIds) {
        if (carIds.isEmpty()) {
            return Map.of();
        }
        final List<Listing> listings = bindParams(em.createQuery(
                "FROM Listing l WHERE l.car.id IN :carIds "
                + "AND l.id = (SELECT MAX(l2.id) FROM Listing l2 WHERE l2.car = l.car)",
                Listing.class), Map.of("carIds", carIds)).getResultList();
        return listings.stream()
                .collect(Collectors.toMap(l -> l.getCar().getId(), Function.identity(), (a, b) -> a));
    }

    private static CarCard toCarCard(final Car car, final long imageId, final Listing listing) {
        final Long listingId   = listing != null ? listing.getId() : null;
        final BigDecimal dayPrice = listing != null ? listing.getDayPrice() : null;
        return new CarCard(
                car.getId(),
                car.getBrand(),
                car.getModel(),
                imageId,
                listingId,
                dayPrice,
                car.getStatus(),
                car.getRatingAvg().orElse(null));
    }

    // -------------------------------------------------------------------------
    // Filter helpers
    // -------------------------------------------------------------------------

    private static final Map<String, String> CAR_SORT_COLUMNS = Map.of(
            "price",  "l.day_price",
            "date",   "c.id",
            "rating", "c.rating_avg"
    );

    private static String buildCarOrderBy(final String sortBy, final String sortDirection) {
        final String col = CAR_SORT_COLUMNS.getOrDefault(sortBy, "c.id");
        final String dir = "asc".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        return col + " " + dir + " NULLS LAST, c.id ASC";
    }

    /** Filters appended to the JPQL COUNT query. Uses JPQL navigation paths. */
    private static void appendOwnerCarJpqlFilters(
            final StringBuilder jpql,
            final Map<String, Object> params,
            final OwnerListingSearchCriteria criteria) {
        if (!criteria.getListingStatusFilters().isEmpty()) {
            jpql.append("AND c.status IN (:ownerCarStatuses) ");
            params.put("ownerCarStatuses", criteria.getListingStatusFilters().stream()
                    .map(s -> Car.Status.valueOf(s.toUpperCase()))
                    .collect(Collectors.toList()));
        }
        final String textQuery = criteria.getTextQuery();
        if (textQuery != null) {
            final String q = "%" + escapeLike(textQuery) + "%";
            jpql.append("AND (LOWER(c.carModel.brand.name) LIKE LOWER(:ownerCarSearch) ESCAPE '\\' "
                    + "OR LOWER(c.carModel.name) LIKE LOWER(:ownerCarSearch) ESCAPE '\\') ");
            params.put("ownerCarSearch", q);
        }
        if (!criteria.getCarTypes().isEmpty()) {
            jpql.append("AND c.type IN (:ownerCarTypes) ");
            params.put("ownerCarTypes", criteria.getCarTypes());
        }
        if (!criteria.getTransmissions().isEmpty()) {
            jpql.append("AND c.transmission IN (:ownerTransmissions) ");
            params.put("ownerTransmissions", criteria.getTransmissions());
        }
        if (!criteria.getPowertrains().isEmpty()) {
            jpql.append("AND c.powertrain IN (:ownerPowertrains) ");
            params.put("ownerPowertrains", criteria.getPowertrains());
        }
        if (criteria.getMinPrice() != null) {
            jpql.append("AND l.dayPrice >= :ownerMinPrice ");
            params.put("ownerMinPrice", criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() != null) {
            jpql.append("AND l.dayPrice <= :ownerMaxPrice ");
            params.put("ownerMaxPrice", criteria.getMaxPrice());
        }
        appendOwnerCarJpqlRatingBandFilter(jpql, criteria.getRatingBands());
    }

    private static void appendOwnerCarJpqlRatingBandFilter(
            final StringBuilder jpql, final List<String> ratingBands) {
        if (ratingBands.isEmpty()) {
            return;
        }
        final List<String> conditions = new ArrayList<>();
        if (ratingBands.contains("UNDER_2")) {
            conditions.add("c.ratingAvg < 2");
        }
        if (ratingBands.contains("2_TO_3")) {
            conditions.add("(c.ratingAvg >= 2 AND c.ratingAvg < 3)");
        }
        if (ratingBands.contains("3_TO_4")) {
            conditions.add("(c.ratingAvg >= 3 AND c.ratingAvg < 4)");
        }
        if (ratingBands.contains("OVER_4")) {
            conditions.add("c.ratingAvg >= 4");
        }
        if (!conditions.isEmpty()) {
            jpql.append("AND (").append(String.join(" OR ", conditions)).append(") ");
        }
    }

    /** Filters appended to the native SQL IDs query. Uses SQL column names. */
    private static void appendOwnerCarNativeSqlFilters(
            final StringBuilder sql,
            final Map<String, Object> params,
            final OwnerListingSearchCriteria criteria) {
        if (!criteria.getListingStatusFilters().isEmpty()) {
            sql.append("AND LOWER(c.status) IN (:ownerCarStatuses) ");
            params.put("ownerCarStatuses", criteria.getListingStatusFilters());
        }
        final String textQuery = criteria.getTextQuery();
        if (textQuery != null) {
            final String q = "%" + escapeLike(textQuery) + "%";
            sql.append("AND (LOWER(COALESCE(cb.name, c.brand, '')) LIKE LOWER(:ownerCarSearch) ESCAPE '\\' "
                    + "OR LOWER(COALESCE(cm.name, c.model, '')) LIKE LOWER(:ownerCarSearch) ESCAPE '\\') ");
            params.put("ownerCarSearch", q);
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
        if (criteria.getExcludeCarId() != null) {
            sql.append("AND c.id <> :excludeCarId ");
            params.put("excludeCarId", criteria.getExcludeCarId());
        }
        appendOwnerCarNativeSqlRatingBandFilter(sql, criteria.getRatingBands());
    }

    private static void appendOwnerCarNativeSqlRatingBandFilter(
            final StringBuilder sql, final List<String> ratingBands) {
        if (ratingBands.isEmpty()) {
            return;
        }
        final List<String> conditions = new ArrayList<>();
        if (ratingBands.contains("UNDER_2")) {
            conditions.add("c.rating_avg < 2");
        }
        if (ratingBands.contains("2_TO_3")) {
            conditions.add("(c.rating_avg >= 2 AND c.rating_avg < 3)");
        }
        if (ratingBands.contains("3_TO_4")) {
            conditions.add("(c.rating_avg >= 3 AND c.rating_avg < 4)");
        }
        if (ratingBands.contains("OVER_4")) {
            conditions.add("c.rating_avg >= 4");
        }
        if (!conditions.isEmpty()) {
            sql.append("AND (").append(String.join(" OR ", conditions)).append(") ");
        }
    }

    private static String escapeLike(final String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Override
    @Transactional
    public void setCarStatus(final long carId, final Car.Status newStatus) {
        final Car car = em.find(Car.class, carId);
        if (car != null) {
            car.setStatus(newStatus);
            car.setUpdatedAt(OffsetDateTime.now());
        }
    }

    @Override
    public List<Car> findCarsByOwnerAndStatuses(final long ownerId, final Collection<Car.Status> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return em.createQuery(
                        "FROM Car c LEFT JOIN FETCH c.carModel m LEFT JOIN FETCH m.brand "
                                + "WHERE c.owner.id = :ownerId AND c.status IN :statuses",
                        Car.class)
                .setParameter("ownerId", ownerId)
                .setParameter("statuses", statuses)
                .getResultList();
    }

    @Override
    public List<Car> findCarsByStatus(final Car.Status status) {
        return em.createQuery(
                        "FROM Car c LEFT JOIN FETCH c.carModel m LEFT JOIN FETCH m.brand WHERE c.status = :status",
                        Car.class)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    @Transactional
    public boolean updateCarStatusIfCurrent(
            final long carId, final Car.Status newStatus, final Car.Status expected) {
        final Car car = em.find(Car.class, carId);
        if (car == null || car.getStatus() != expected) {
            return false;
        }
        car.setStatus(newStatus);
        car.setUpdatedAt(OffsetDateTime.now());
        return true;
    }

    @Override
    public List<CarCard> findSimilarCarCards(
            final long carId,
            final int limit,
            final java.time.LocalDate browseWallDate,
            final Long excludeOwnerUserId) {
        final Optional<Car> anchorOpt = getCarById(carId);
        if (anchorOpt.isEmpty()) {
            return List.of();
        }
        final Car ref = anchorOpt.get();

        final Map<String, Object> params = new HashMap<>();
        params.put("activeStatus", Car.Status.ACTIVE);
        params.put("carId", carId);
        params.put("refType", ref.getType().name());
        params.put("refPowertrain", ref.getPowertrain().name());
        params.put("refTransmission", ref.getTransmission().name());

        final StringBuilder idSql = new StringBuilder(
                "SELECT c.id FROM cars c "
                + "JOIN listing_availability la ON la.car_id = c.id "
                + "WHERE c.status = :activeStatus "
                + "AND c.id <> :carId "
                + "AND UPPER(c.type) = :refType "
                + "AND UPPER(c.powertrain) = :refPowertrain "
                + "AND UPPER(c.transmission) = :refTransmission ");
        if (browseWallDate != null) {
            idSql.append("AND la.end_inclusive >= :browseWallDate ");
            params.put("browseWallDate", browseWallDate);
        }
        if (excludeOwnerUserId != null) {
            idSql.append("AND c.owner_id <> :excludeOwnerUserId ");
            params.put("excludeOwnerUserId", excludeOwnerUserId);
        }
        idSql.append("GROUP BY c.id ORDER BY c.rating_avg DESC NULLS LAST, c.id ASC");

        @SuppressWarnings("unchecked")
        final List<Number> ids = bindParams(em.createNativeQuery(idSql.toString()), params)
                .setMaxResults(limit * 4)
                .getResultList();

        if (ids.isEmpty()) {
            return List.of();
        }

        final List<Long> idLongs = ids.stream().map(Number::longValue).collect(Collectors.toList());
        @SuppressWarnings("unchecked")
        final List<Car> cars = em.createQuery(
                        "SELECT c FROM Car c LEFT JOIN FETCH c.carModel cm LEFT JOIN FETCH cm.brand "
                        + "WHERE c.id IN :ids", Car.class)
                .setParameter("ids", idLongs)
                .getResultList();

        final Map<Long, Car> carById = cars.stream().collect(Collectors.toMap(Car::getId, Function.identity()));

        // Load first image per car
        @SuppressWarnings("unchecked")
        final List<Object[]> imgRows = em.createNativeQuery(
                        "SELECT cp.car_id, MIN(cp.image_id) FROM car_pictures cp "
                        + "WHERE cp.car_id IN :ids GROUP BY cp.car_id")
                .setParameter("ids", idLongs)
                .getResultList();
        final Map<Long, Long> imageMap = new HashMap<>();
        for (final Object[] row : imgRows) {
            imageMap.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }

        // Load min availability price per car
        @SuppressWarnings("unchecked")
        final List<Object[]> priceRows = em.createNativeQuery(
                        "SELECT la.car_id, MIN(la.day_price) FROM listing_availability la "
                        + "WHERE la.car_id IN :ids AND la.kind = 'offered' GROUP BY la.car_id")
                .setParameter("ids", idLongs)
                .getResultList();
        final Map<Long, BigDecimal> priceMap = new HashMap<>();
        for (final Object[] row : priceRows) {
            if (row[1] != null) {
                priceMap.put(((Number) row[0]).longValue(),
                        row[1] instanceof BigDecimal ? (BigDecimal) row[1] : new BigDecimal(row[1].toString()));
            }
        }

        return idLongs.stream()
                .limit(limit)
                .map(carById::get)
                .filter(Objects::nonNull)
                .map(c -> new CarCard(
                        c.getId(),
                        c.getBrand(),
                        c.getModel(),
                        imageMap.getOrDefault(c.getId(), 0L),
                        null,
                        priceMap.get(c.getId()),
                        c.getStatus(),
                        c.getRatingAvg().orElse(null)))
                .collect(Collectors.toList());
    }
}
