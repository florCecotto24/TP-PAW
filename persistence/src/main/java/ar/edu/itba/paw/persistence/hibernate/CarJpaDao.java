package ar.edu.itba.paw.persistence.hibernate;

import static ar.edu.itba.paw.persistence.util.JpaQueryUtils.bindParams;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.util.OwnerListingSearchCriteria;
import ar.edu.itba.paw.persistence.CarDao;

@Transactional
@Repository
public class CarJpaDao implements CarDao {

    @PersistenceContext
    private EntityManager em;

    private final int carCatalogLimit;

    public CarJpaDao(@Value("${app.listing.car-catalog-limit:8}") final int carCatalogLimit) {
        this.carCatalogLimit = Math.max(1, carCatalogLimit);
    }

    @Override
    public Car createCar(final long ownerId, final String plate, final String brand, final String model,
                         final Car.Type type, final Car.Powertrain powertrain, final Car.Transmission transmission) {
        final User ownerRef = em.getReference(User.class, ownerId);
        final Car car = Car.builder()
                .owner(ownerRef)
                .plate(plate)
                .brand(brand)
                .model(model)
                .type(type)
                .powertrain(powertrain)
                .transmission(transmission)
                .build();
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
        return Optional.ofNullable(em.find(Car.class, id));
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
        final String sql = "SELECT c.id FROM cars c INNER JOIN listings l ON l.car_id = c.id WHERE l.status = '"
                + Listing.Status.ACTIVE.name().toLowerCase() + "' "
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
        final Map<String, Object> qParams = new HashMap<>();
        qParams.put("ids", orderedCarIds);
        final List<Car> cars =
                bindParams(em.createQuery("FROM Car c WHERE c.id IN :ids", Car.class), qParams).getResultList();
        final Map<Long, Car> byId =
                cars.stream().collect(Collectors.toMap(Car::getId, Function.identity(), (a, b) -> a));
        return orderedCarIds.stream().map(byId::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarCard> getOwnerCarCards(final OwnerListingSearchCriteria criteria) {
        final int page = criteria.getPage();
        final int pageSize = criteria.getPageSize();

        final Map<String, Object> countParams = new HashMap<>();
        countParams.put("ownerId", criteria.getOwnerId());
        final StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(*) FROM cars c "
                + "LEFT JOIN listings l ON l.id = (SELECT MAX(id) FROM listings WHERE car_id = c.id) "
                + "WHERE c.owner_id = :ownerId ");
        appendOwnerCarFilters(countSql, countParams, criteria);
        final Number total = (Number) bindParams(em.createNativeQuery(countSql.toString()), countParams).getSingleResult();

        final int offset = page * pageSize;
        final Map<String, Object> listParams = new HashMap<>();
        listParams.put("ownerId", criteria.getOwnerId());
        listParams.put("limit", pageSize);
        listParams.put("offset", offset);
        final StringBuilder listSql = new StringBuilder(
                "SELECT c.id AS car_id, c.brand, c.model, "
                + "(SELECT cp.image_id FROM car_pictures cp WHERE cp.car_id = c.id "
                + "ORDER BY cp.display_order ASC LIMIT 1) AS image_id, "
                + "l.id AS listing_id, l.day_price, l.status AS listing_status, l.rating_avg "
                + "FROM cars c "
                + "LEFT JOIN listings l ON l.id = (SELECT MAX(id) FROM listings WHERE car_id = c.id) "
                + "WHERE c.owner_id = :ownerId ");
        appendOwnerCarFilters(listSql, listParams, criteria);
        listSql.append("ORDER BY ").append(buildCarOrderBy(criteria.getSortBy(), criteria.getSortDirection()))
               .append(" LIMIT :limit OFFSET :offset");
        final List<CarCard> content = runCarCardNativeQuery(listSql.toString(), listParams);
        return new Page<>(content, page, pageSize, total != null ? total.longValue() : 0L);
    }

    private static final Map<String, String> CAR_SORT_COLUMNS = Map.of(
            "price",  "l.day_price",
            "date",   "c.id",
            "rating", "l.rating_avg"
    );

    private static String buildCarOrderBy(final String sortBy, final String sortDirection) {
        final String col = CAR_SORT_COLUMNS.getOrDefault(sortBy, "c.id");
        final String dir = "asc".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        return col + " " + dir + " NULLS LAST, c.id ASC";
    }

    private static void appendOwnerCarFilters(
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
            sql.append("AND (LOWER(c.brand) LIKE LOWER(:ownerCarSearch) ESCAPE '\\' "
                    + "OR LOWER(c.model) LIKE LOWER(:ownerCarSearch) ESCAPE '\\') ");
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
        appendOwnerCarRatingBandFilter(sql, criteria.getRatingBands());
    }

    private static void appendOwnerCarRatingBandFilter(final StringBuilder sql, final List<String> ratingBands) {
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

    @SuppressWarnings("unchecked")
    private List<CarCard> runCarCardNativeQuery(final String sql, final Map<String, Object> params) {
        final List<Object[]> rows = bindParams(em.createNativeQuery(sql), params).getResultList();
        final List<CarCard> result = new ArrayList<>(rows.size());
        for (final Object[] row : rows) {
            result.add(mapCarCard(row));
        }
        return result;
    }

    private static CarCard mapCarCard(final Object[] row) {
        final long carId = ((Number) row[0]).longValue();
        final String brand = (String) row[1];
        final String model = (String) row[2];
        final Object rawImageId = row[3];
        final long imageId = rawImageId == null ? 0L : ((Number) rawImageId).longValue();
        final Object rawListingId = row[4];
        final Long listingId = rawListingId == null ? null : ((Number) rawListingId).longValue();
        final BigDecimal dayPrice = (BigDecimal) row[5];
        final String statusStr = (String) row[6];
        final Listing.Status status = statusStr == null ? null : Listing.Status.valueOf(statusStr.toUpperCase());
        final Object rawRating = row[7];
        final BigDecimal ratingAvg = rawRating == null ? null
                : new BigDecimal(rawRating.toString()).setScale(2, RoundingMode.HALF_UP);
        return new CarCard(carId, brand, model, imageId, listingId, dayPrice, status, ratingAvg);
    }

    private static String escapeLike(final String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
