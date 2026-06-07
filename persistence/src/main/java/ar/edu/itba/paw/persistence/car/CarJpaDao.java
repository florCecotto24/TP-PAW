package ar.edu.itba.paw.persistence.car;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.car.CarPriceMarketInsight;
import ar.edu.itba.paw.models.pagination.DualLayerPageWindow;
import ar.edu.itba.paw.models.util.search.CarSearchCriteria;
import ar.edu.itba.paw.models.util.search.OwnerCarSearchCriteria;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.persistence.util.DbPaginationConfig;
import static ar.edu.itba.paw.persistence.util.JpaQueryUtils.bindParams;

@Transactional(readOnly = true)
@Repository
public class CarJpaDao implements CarDao {

    private static final ZoneId WALL_ZONE = AppTimezone.WALL_ZONE;

    /**
     * Hides cars whose owner has the {@code blocked} flag set from every public/browse query
     * (catalog, search, similar, market insight). Inserted right after {@code FROM cars c}.
     * Owners stay able to see their own listings via separate owner-hub queries.
     */
    private static final String OWNER_NOT_BLOCKED_JOIN =
            "INNER JOIN users u ON u.id = c.owner_id AND u.blocked = FALSE ";

    // String constants derived from the entity enums via name().toLowerCase() so that renaming a
    // Car.Status / Reservation.Status / CarAvailability.Kind value forces a compile-time update of
    // every native query that references it (rather than letting a stale 'active' / 'offered'
    // literal silently match nothing). Mirrors the StatusConverter / KindConverter persistence
    // contract: enum name lower-cased is what hits the database.
    private static final String STATUS_ACTIVE = enumDbValue(Car.Status.ACTIVE);
    private static final String KIND_OFFERED = enumDbValue(CarAvailability.Kind.OFFERED);
    private static final String ACTIVE_RESERVATION_STATUS_CSV = quotedEnumCsv(
            Reservation.Status.PENDING, Reservation.Status.ACCEPTED, Reservation.Status.STARTED);
    private static final String CANCELLED_BY_PARTICIPANT_STATUS_CSV = quotedEnumCsv(
            Reservation.Status.CANCELLED_BY_OWNER, Reservation.Status.CANCELLED_BY_RIDER);

    private static String enumDbValue(final Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private static String quotedEnumCsv(final Enum<?>... values) {
        return Arrays.stream(values)
                .map(CarJpaDao::enumDbValue)
                .map(s -> "'" + s + "'")
                .collect(Collectors.joining(", "));
    }

    @PersistenceContext
    private EntityManager em;

    private final CarPictureDao carPictureDao;
    private final CarAvailabilityDao carAvailabilityDao;
    private final DbPaginationConfig dbPaginationConfig;

    @org.springframework.beans.factory.annotation.Autowired
    public CarJpaDao(final CarPictureDao carPictureDao,
                     final CarAvailabilityDao carAvailabilityDao,
                     final DbPaginationConfig dbPaginationConfig) {
        this.carPictureDao = carPictureDao;
        this.carAvailabilityDao = carAvailabilityDao;
        this.dbPaginationConfig = dbPaginationConfig;
    }

    @Override
    @Transactional
    public Car createCar(final long ownerId, final String plate, final long carModelId,
                         final Integer year, final Car.Powertrain powertrain,
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
                .year(year)
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

    // -------------------------------------------------------------------------
    // Owner car cards — 1+1 pattern
    // -------------------------------------------------------------------------

    @Override
    public Page<CarCard> getOwnerCarCards(final OwnerCarSearchCriteria criteria) {
        final int page  = criteria.getPage();
        final int pageSize = criteria.getPageSize();

        // Step 1: COUNT via JPQL (no native SQL)
        final Map<String, Object> countParams = new HashMap<>();
        countParams.put("ownerId", criteria.getOwnerId());
        final StringBuilder countJpql = new StringBuilder(
                "SELECT COUNT(DISTINCT c.id) FROM Car c "
                + "WHERE c.owner.id = :ownerId ");
        appendOwnerCarJpqlFilters(countJpql, countParams, criteria);
        final long total = (Long) bindParams(em.createQuery(countJpql.toString()), countParams).getSingleResult();

        // Step 2: paginated car IDs + min price via native SQL (LEFT JOIN car_availability for ordering/price)
        final int offset = page * pageSize;
        final Map<String, Object> idsParams = new HashMap<>();
        idsParams.put("ownerId", criteria.getOwnerId());
        idsParams.put("limit", pageSize);
        idsParams.put("offset", offset);
        // When prioritizeRefundPending is set, expose a 0/1 column via EXISTS on the reservations table
        // and prepend it to the ORDER BY. The EXISTS subquery only touches reservations rows for this
        // car so it stays cheap with the (car_id, status) index. Keeping it in the same paginated query
        // (rather than a separate fetch + in-memory shuffle) preserves correctness across pages.
        final String pendingRefundProjection = criteria.isPrioritizeRefundPending()
                ? ", MAX(CASE WHEN EXISTS (SELECT 1 FROM reservations r WHERE r.car_id = c.id "
                        + "AND r.payment_refund_required = TRUE "
                        + "AND r.payment_refund_receipt_file_id IS NULL "
                        + "AND r.status IN (" + CANCELLED_BY_PARTICIPANT_STATUS_CSV + ")) THEN 1 ELSE 0 END) AS pending_refund "
                : "";
        final StringBuilder idsSql = new StringBuilder(
                "SELECT c.id, MIN(la.day_price) AS min_price")
                .append(pendingRefundProjection)
                .append(" FROM cars c "
                + "LEFT JOIN car_availability la ON la.car_id = c.id AND la.kind = '" + KIND_OFFERED + "' "
                + "LEFT JOIN car_models cm ON cm.id = c.model_id "
                + "LEFT JOIN car_brands cb ON cb.id = cm.brand_id "
                + "WHERE c.owner_id = :ownerId ");
        appendOwnerCarNativeSqlFilters(idsSql, idsParams, criteria);
        idsSql.append("GROUP BY c.id, c.created_at, c.rating_avg ")
              .append("ORDER BY ");
        if (criteria.isPrioritizeRefundPending()) {
            idsSql.append("pending_refund DESC, ");
        }
        idsSql.append(buildCarOrderBy(criteria.getSortBy(), criteria.getSortDirection()))
              .append(" LIMIT :limit OFFSET :offset");
        @SuppressWarnings("unchecked")
        final List<Object[]> rows = bindParams(em.createNativeQuery(idsSql.toString()), idsParams).getResultList();
        final List<Long> orderedCarIds = new ArrayList<>();
        final Map<Long, BigDecimal> priceById = new HashMap<>();
        for (final Object[] row : rows) {
            final long id = ((Number) row[0]).longValue();
            orderedCarIds.add(id);
            if (row[1] != null) {
                priceById.put(id, toBigDecimal(row[1]));
            }
        }

        // Step 3: load entities and assemble cards
        final List<CarCard> content = orderedCarIds.isEmpty()
                ? List.of()
                : assembleCarCards(orderedCarIds, priceById);
        return new Page<>(content, page, pageSize, total);
    }

    private Map<Long, Long> loadCoverImageIdByCarIds(final Collection<Long> carIds) {
        // Pictures belong to CarPicture, so we delegate ownership of that query to its DAO.
        return carPictureDao.findCoverImageIdsByCarIds(carIds);
    }

    // -------------------------------------------------------------------------
    // Filter helpers
    // -------------------------------------------------------------------------

    private static final Map<String, String> CAR_SORT_COLUMNS = Map.of(
            "price",  "min_price",
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
            final OwnerCarSearchCriteria criteria) {
        if (!criteria.getCarStatusFilters().isEmpty()) {
            jpql.append("AND c.status IN (:ownerCarStatuses) ");
            params.put("ownerCarStatuses", criteria.getCarStatusFilters().stream()
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
            jpql.append("AND c.carModel.type IN (:ownerCarTypes) ");
            params.put("ownerCarTypes", criteria.getCarTypes().stream()
                    .map(s -> Car.Type.valueOf(s.toUpperCase()))
                    .collect(Collectors.toList()));
        }
        if (!criteria.getTransmissions().isEmpty()) {
            jpql.append("AND c.transmission IN (:ownerTransmissions) ");
            params.put("ownerTransmissions", criteria.getTransmissions().stream()
                    .map(s -> Car.Transmission.valueOf(s.toUpperCase()))
                    .collect(Collectors.toList()));
        }
        if (!criteria.getPowertrains().isEmpty()) {
            jpql.append("AND c.powertrain IN (:ownerPowertrains) ");
            params.put("ownerPowertrains", criteria.getPowertrains().stream()
                    .map(s -> Car.Powertrain.valueOf(s.toUpperCase()))
                    .collect(Collectors.toList()));
        }
        if (criteria.getMinPrice() != null || criteria.getMaxPrice() != null) {
            params.put("ownerOfferedKind", CarAvailability.Kind.OFFERED);
        }
        if (criteria.getMinPrice() != null) {
            jpql.append("AND EXISTS (SELECT 1 FROM CarAvailability la WHERE la.car = c "
                    + "AND la.kind = :ownerOfferedKind AND la.dayPrice >= :ownerMinPrice) ");
            params.put("ownerMinPrice", criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() != null) {
            jpql.append("AND EXISTS (SELECT 1 FROM CarAvailability la WHERE la.car = c "
                    + "AND la.kind = :ownerOfferedKind AND la.dayPrice <= :ownerMaxPrice) ");
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
            final OwnerCarSearchCriteria criteria) {
        if (!criteria.getCarStatusFilters().isEmpty()) {
            sql.append("AND LOWER(c.status) IN (:ownerCarStatuses) ");
            params.put("ownerCarStatuses", criteria.getCarStatusFilters());
        }
        final String textQuery = criteria.getTextQuery();
        if (textQuery != null) {
            final String q = "%" + escapeLike(textQuery) + "%";
            sql.append("AND (LOWER(COALESCE(cb.name, '')) LIKE LOWER(:ownerCarSearch) ESCAPE '\\' "
                    + "OR LOWER(COALESCE(cm.name, '')) LIKE LOWER(:ownerCarSearch) ESCAPE '\\') ");
            params.put("ownerCarSearch", q);
        }
        if (!criteria.getCarTypes().isEmpty()) {
            sql.append("AND cm.type IN (:ownerCarTypes) ");
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
            sql.append("AND la.day_price >= :ownerMinPrice ");
            params.put("ownerMinPrice", criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() != null) {
            sql.append("AND la.day_price <= :ownerMaxPrice ");
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
    public Page<Car> findAllCarsPaginated(final int page, final int pageSize) {
        final int safePage = Math.max(0, page);
        final int safePageSize = Math.max(1, pageSize);
        final long total = em.createQuery("SELECT COUNT(c) FROM Car c", Long.class)
                .getSingleResult();
        if (total == 0L) {
            return new Page<>(List.of(), safePage, safePageSize, 0L);
        }
        final List<Long> carIds = em.createQuery(
                        "SELECT c.id FROM Car c ORDER BY c.createdAt DESC, c.id DESC", Long.class)
                .setFirstResult(safePage * safePageSize)
                .setMaxResults(safePageSize)
                .getResultList();
        if (carIds.isEmpty()) {
            return new Page<>(List.of(), safePage, safePageSize, total);
        }
        final List<Car> cars = em.createQuery(
                        "FROM Car c "
                                + "LEFT JOIN FETCH c.owner o "
                                + "LEFT JOIN FETCH c.carModel m "
                                + "LEFT JOIN FETCH m.brand "
                                + "WHERE c.id IN :ids",
                        Car.class)
                .setParameter("ids", carIds)
                .getResultList();
        final Map<Long, Car> carsById = cars.stream()
                .collect(Collectors.toMap(Car::getId, Function.identity(), (left, right) -> left));
        final List<Car> orderedCars = carIds.stream()
                .map(carsById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new Page<>(orderedCars, safePage, safePageSize, total);
    }

    @Override
    public List<Car> findCarsByModelId(final long modelId) {
        return em.createQuery(
                        "FROM Car c WHERE c.carModel.id = :modelId", Car.class)
                .setParameter("modelId", modelId)
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
    @Transactional
    public void updateInsuranceDocument(final long carId, final long insuranceFileId) {
        final Car car = em.find(Car.class, carId);
        if (car == null) {
            return;
        }
        final StoredFile file = em.getReference(StoredFile.class, insuranceFileId);
        car.setInsuranceFile(file);
        car.setUpdatedAt(OffsetDateTime.now());
    }

    @Override
    @Transactional
    public void clearInsuranceDocument(final long carId) {
        final Car car = em.find(Car.class, carId);
        if (car == null) {
            return;
        }
        car.setInsuranceFile(null);
        car.setUpdatedAt(OffsetDateTime.now());
    }

    @Override
    @Transactional
    public void updateMinimumRentalDays(final long carId, final int days) {
        final Car car = em.find(Car.class, carId);
        if (car != null) {
            car.setMinimumRentalDays(days);
            car.setUpdatedAt(OffsetDateTime.now());
        }
    }

    @Override
    @Transactional
    public void updateRatingAvg(final long carId, final BigDecimal average) {
        final Car car = em.find(Car.class, carId);
        if (car != null) {
            car.setRatingAvg(average);
            car.setUpdatedAt(OffsetDateTime.now());
        }
    }

    @Override
    public List<CarCard> findSimilarCarCards(
            final long carId,
            final int limit,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId) {
        final Optional<Car> anchorOpt = getCarById(carId);
        if (anchorOpt.isEmpty()) {
            return List.of();
        }
        final Car ref = anchorOpt.get();

        final Car.Type refType = ref.getType();
        final Map<String, Object> params = new HashMap<>();
        params.put("carId", carId);
        params.put("refPowertrain", ref.getPowertrain().name());
        params.put("refTransmission", ref.getTransmission().name());

        final StringBuilder idSql = new StringBuilder(
                "SELECT c.id FROM cars c "
                + OWNER_NOT_BLOCKED_JOIN
                + "JOIN car_availability la ON la.car_id = c.id "
                + "JOIN car_models cm ON cm.id = c.model_id "
                + "WHERE c.status = '" + STATUS_ACTIVE + "' "
                + "AND c.id <> :carId "
                + "AND UPPER(c.powertrain) = :refPowertrain "
                + "AND UPPER(c.transmission) = :refTransmission ");
        if (refType != null) {
            idSql.append("AND UPPER(cm.type) = :refType ");
            params.put("refType", refType.name());
        }
        if (browseWallDate != null) {
            idSql.append("AND la.end_date >= :browseWallDate ");
            params.put("browseWallDate", browseWallDate);
        }
        if (excludeOwnerUserId != null) {
            idSql.append("AND c.owner_id <> :excludeOwnerUserId ");
            params.put("excludeOwnerUserId", excludeOwnerUserId);
        }
        idSql.append("GROUP BY c.id ORDER BY c.rating_avg DESC NULLS LAST, c.id ASC");

        // The id-only query already encodes every WHERE clause that gates a similar listing
        // (active status, owner not blocked, availability window, type/transmission/powertrain
        // match, optional browseWallDate, optional excludeOwnerUserId). The downstream filter is
        // only {@code filter(Objects::nonNull)}, which can drop a row solely if the car was hard-
        // deleted between the id query and the entity-fetch JPQL — practically impossible inside
        // the same transaction. Pulling 4× the requested limit was loading three quarters of the
        // rows into memory just to discard them. Cap at the exact limit.
        @SuppressWarnings("unchecked")
        final List<Number> ids = bindParams(em.createNativeQuery(idSql.toString()), params)
                .setMaxResults(limit)
                .getResultList();

        if (ids.isEmpty()) {
            return List.of();
        }

        final List<Long> idLongs = ids.stream().map(Number::longValue).collect(Collectors.toList());
        @SuppressWarnings("unchecked")
        final List<Car> cars = em.createQuery(
                        "FROM Car c LEFT JOIN FETCH c.carModel cm LEFT JOIN FETCH cm.brand "
                        + "WHERE c.id IN :ids", Car.class)
                .setParameter("ids", idLongs)
                .getResultList();

        final Map<Long, Car> carById = cars.stream().collect(Collectors.toMap(Car::getId, Function.identity()));

        // Cover image and "from" price are owned by other DAOs; delegate to them.
        final Map<Long, Long> imageMap = carPictureDao.findCoverImageIdsByCarIds(idLongs);
        final Map<Long, BigDecimal> priceMap = carAvailabilityDao.findMinOfferedDayPriceByCarIds(idLongs);

        return idLongs.stream()
                .limit(limit)
                .map(carById::get)
                .filter(Objects::nonNull)
                .map(c -> CarCard.builder()
                        .carId(c.getId())
                        .brand(c.getBrand())
                        .model(c.getModel())
                        .imageId(imageMap.getOrDefault(c.getId(), 0L))
                        .dayPrice(priceMap.get(c.getId()))
                        .status(c.getStatus())
                        .ratingAvg(c.getRatingAvg().orElse(null))
                        .build())
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Public car-card browse + search (replaces ListingJpaDao queries)
    // -------------------------------------------------------------------------

    private static final Map<String, String> CAR_BROWSE_SORT_COLUMNS = Map.of(
            "price",  "min_price",
            "date",   "c.created_at",
            "rating", "c.rating_avg"
    );

    @Override
    public Page<CarCard> getCheapestCarCards(
            final int page, final int pageSize,
            final LocalDate browseWallDate, final Long excludeOwnerUserId) {
        return browseCarCardsPage(
                "min_price ASC, c.id ASC", page, pageSize, browseWallDate, excludeOwnerUserId);
    }

    @Override
    public Page<CarCard> getMostRecentCarCards(
            final int page, final int pageSize,
            final LocalDate browseWallDate, final Long excludeOwnerUserId) {
        return browseCarCardsPage(
                "c.created_at DESC, c.id ASC", page, pageSize, browseWallDate, excludeOwnerUserId);
    }

    /**
     * Owns the full browse-page pipeline: the COUNT (JPQL navigating entities), the paginated
     * window (native SQL only for {@code LIMIT/OFFSET}) and the assembly into {@link CarCard}.
     * Callers receive a ready-to-render {@link Page}; the service layer does not compute offsets
     * or assemble the page object.
     */
    private Page<CarCard> browseCarCardsPage(
            final String orderBy,
            final int page,
            final int pageSize,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId) {
        final int safePage = Math.max(0, page);
        final int safePageSize = Math.max(1, pageSize);
        final long total = countBrowseEligibleActiveCars(browseWallDate, excludeOwnerUserId);
        final List<CarCard> content = loadCarCardsWindow(
                orderBy, safePage * safePageSize, safePageSize,
                browseWallDate, excludeOwnerUserId, null);
        return new Page<>(content, safePage, safePageSize, total);
    }

    private long countBrowseEligibleActiveCars(final LocalDate browseWallDate, final Long excludeOwnerUserId) {
        final Map<String, Object> params = new HashMap<>();
        final StringBuilder jpql = new StringBuilder(
                "SELECT COUNT(DISTINCT la.car) FROM CarAvailability la "
                + "JOIN la.car c "
                + "JOIN c.owner u "
                + "JOIN c.carModel cm "
                + "JOIN cm.brand cb "
                + "WHERE u.blocked = FALSE "
                + "AND cm.validated = TRUE "
                + "AND cb.validated = TRUE "
                + "AND la.kind = :offeredKind "
                + "AND c.status = :activeStatus ");
        params.put("offeredKind", CarAvailability.Kind.OFFERED);
        params.put("activeStatus", Car.Status.ACTIVE);
        if (browseWallDate != null) {
            jpql.append("AND la.endInclusive >= :browseWallDate ");
            params.put("browseWallDate", browseWallDate);
        }
        if (excludeOwnerUserId != null) {
            jpql.append("AND c.owner.id <> :excludeOwnerUserId ");
            params.put("excludeOwnerUserId", excludeOwnerUserId);
        }
        final Number count = (Number) bindParams(em.createQuery(jpql.toString()), params).getSingleResult();
        return count == null ? 0L : count.longValue();
    }

    @Override
    public Page<CarCard> searchCarCards(final CarSearchCriteria criteria) {
        final DualLayerPageWindow w = DualLayerPageWindow.compute(
                criteria.getPage(), criteria.getUiPageSize(), dbPaginationConfig.getDbFetchSize());

        final Map<String, Object> countParams = new HashMap<>();
        final StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(DISTINCT c.id) FROM cars c "
                + OWNER_NOT_BLOCKED_JOIN
                + "INNER JOIN car_models cm ON cm.id = c.model_id AND cm.validated = TRUE "
                + "INNER JOIN car_brands cb ON cb.id = cm.brand_id AND cb.validated = TRUE "
                + "INNER JOIN car_availability la ON la.car_id = c.id AND la.kind = '" + KIND_OFFERED + "' "
                + "WHERE c.status = '" + STATUS_ACTIVE + "' ");
        appendBrowseEligibilityFilters(countSql, countParams, criteria.getBrowseWallDate(), criteria.getExcludeOwnerUserId());
        appendCarSearchFilters(countSql, countParams, criteria);
        final long total = ((Number) bindParams(em.createNativeQuery(countSql.toString()), countParams).getSingleResult()).longValue();

        final String orderBy = buildCarBrowseOrderBy(criteria.getSortBy(), criteria.getSortDirection());
        final List<CarCard> batch = loadCarCardsWindow(
                orderBy, w.sqlOffset(), w.sqlLimit(),
                criteria.getBrowseWallDate(), criteria.getExcludeOwnerUserId(), criteria);
        final List<CarCard> slice = DualLayerPageWindow.sliceBatch(batch, w);
        return new Page<>(slice, w.uiPage(), w.uiPageSize(), total);
    }

    @Override
    public Optional<CarPriceMarketInsight> findActiveDayPriceMarketInsightByBrandAndModel(
            final String brand, final String model, final Long excludeCarId) {
        // Aggregates the per-car minimum offered day price across the eligible active listings
        // for {@code brand}+{@code model} in a single round-trip. Previous version pulled every
        // per-car min into memory and folded them in Java, which scales linearly with how popular
        // the model is and adds nothing the database cannot do.
        //
        // We use a derived-table form so the outer aggregate operates on the per-car minima
        // rather than on the raw availability rows: a car with multiple OFFERED periods at
        // different prices must contribute exactly one price (its minimum), otherwise high-price
        // periods would inflate AVG and break MAX.
        final Map<String, Object> params = new HashMap<>();
        params.put("brand", brand);
        params.put("model", model);
        // The persisted kind/status columns are lowercase (see enumDbValue / DB conventions). Other
        // queries in this DAO inline the KIND_OFFERED / STATUS_ACTIVE constants for that reason;
        // do the same here so the bound parameters match what's actually stored.
        final StringBuilder sql = new StringBuilder(
                "SELECT MIN(per_car.min_price), MAX(per_car.min_price), AVG(per_car.min_price), COUNT(*) "
                + "FROM (SELECT MIN(la.day_price) AS min_price "
                + "      FROM car_availability la "
                + "      JOIN cars c ON c.id = la.car_id "
                + "      JOIN users u ON u.id = c.owner_id "
                + "      JOIN car_models cm ON cm.id = c.model_id "
                + "      JOIN car_brands cb ON cb.id = cm.brand_id "
                + "      WHERE u.blocked = FALSE "
                + "      AND cm.validated = TRUE "
                + "      AND cb.validated = TRUE "
                + "      AND la.kind = '" + KIND_OFFERED + "' "
                + "      AND c.status = '" + STATUS_ACTIVE + "' "
                + "      AND LOWER(cb.name) = LOWER(:brand) "
                + "      AND LOWER(cm.name) = LOWER(:model) ");
        if (excludeCarId != null) {
            sql.append("      AND c.id <> :excludeCarId ");
            params.put("excludeCarId", excludeCarId);
        }
        sql.append("      GROUP BY c.id) per_car");

        final Object[] row = (Object[]) bindParams(em.createNativeQuery(sql.toString()), params).getSingleResult();
        if (row == null) {
            return Optional.empty();
        }
        // PostgreSQL returns null for MIN/MAX/AVG when the inner set is empty, and 0 for COUNT.
        final long count = ((Number) row[3]).longValue();
        if (count == 0L || row[0] == null || row[1] == null || row[2] == null) {
            return Optional.empty();
        }
        final BigDecimal min = toBigDecimal(row[0]);
        final BigDecimal max = toBigDecimal(row[1]);
        final BigDecimal avg = toBigDecimal(row[2]).setScale(4, RoundingMode.HALF_UP);
        return Optional.of(new CarPriceMarketInsight(min, max, avg, count));
    }

    // -------------------------------------------------------------------------
    // Browse query helpers
    // -------------------------------------------------------------------------

    private List<CarCard> loadCarCardsWindow(
            final String orderBy,
            final int offset,
            final int limit,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId,
            final CarSearchCriteria searchCriteria) {
        final Map<String, Object> params = new HashMap<>();
        params.put("limit", limit);
        params.put("offset", offset);
        final StringBuilder sql = new StringBuilder(
                "SELECT c.id, MIN(la.day_price) AS min_price FROM cars c "
                + OWNER_NOT_BLOCKED_JOIN
                + "INNER JOIN car_models cm ON cm.id = c.model_id AND cm.validated = TRUE "
                + "INNER JOIN car_brands cb ON cb.id = cm.brand_id AND cb.validated = TRUE "
                + "INNER JOIN car_availability la ON la.car_id = c.id AND la.kind = '" + KIND_OFFERED + "' "
                + "WHERE c.status = '" + STATUS_ACTIVE + "' ");
        appendBrowseEligibilityFilters(sql, params, browseWallDate, excludeOwnerUserId);
        if (searchCriteria != null) {
            appendCarSearchFilters(sql, params, searchCriteria);
        }
        sql.append("GROUP BY c.id, c.created_at, c.rating_avg ")
           .append("ORDER BY ").append(orderBy)
           .append(" LIMIT :limit OFFSET :offset");
        @SuppressWarnings("unchecked")
        final List<Object[]> rows = bindParams(em.createNativeQuery(sql.toString()), params).getResultList();
        if (rows.isEmpty()) {
            return List.of();
        }
        final List<Long> orderedCarIds = new ArrayList<>();
        final Map<Long, BigDecimal> priceById = new HashMap<>();
        for (final Object[] row : rows) {
            final long id = ((Number) row[0]).longValue();
            orderedCarIds.add(id);
            if (row[1] != null) {
                priceById.put(id, toBigDecimal(row[1]));
            }
        }
        return assembleCarCards(orderedCarIds, priceById);
    }

    private List<CarCard> assembleCarCards(
            final List<Long> orderedCarIds, final Map<Long, BigDecimal> priceById) {
        final List<Car> cars = bindParams(em.createQuery(
                "FROM Car c LEFT JOIN FETCH c.carModel m LEFT JOIN FETCH m.brand WHERE c.id IN :ids",
                Car.class), Map.of("ids", orderedCarIds)).getResultList();
        final Map<Long, Car> byId = cars.stream()
                .collect(Collectors.toMap(Car::getId, Function.identity(), (a, b) -> a));
        final Map<Long, Long> imageByCar = loadCoverImageIdByCarIds(orderedCarIds);
        return orderedCarIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(c -> CarCard.builder()
                        .carId(c.getId())
                        .brand(c.getBrand())
                        .model(c.getModel())
                        .imageId(imageByCar.getOrDefault(c.getId(), 0L))
                        .dayPrice(priceById.get(c.getId()))
                        .status(c.getStatus())
                        .ratingAvg(c.getRatingAvg().orElse(null))
                        .modelPendingValidation(c.isModelPendingValidation())
                        .minimumRentalDays(c.getMinimumRentalDays())
                        .ownerId(c.getOwner() == null ? null : c.getOwner().getId())
                        .build())
                .collect(Collectors.toList());
    }

    private static void appendBrowseEligibilityFilters(
            final StringBuilder sql, final Map<String, Object> params,
            final LocalDate browseWallDate, final Long excludeOwnerUserId) {
        if (browseWallDate != null) {
            sql.append("AND la.end_date >= :browseWallDate ");
            params.put("browseWallDate", java.sql.Date.valueOf(browseWallDate));
        }
        if (excludeOwnerUserId != null) {
            sql.append("AND c.owner_id <> :excludeOwnerUserId ");
            params.put("excludeOwnerUserId", excludeOwnerUserId);
        }
    }

    private static void appendCarSearchFilters(
            final StringBuilder sql, final Map<String, Object> params,
            final CarSearchCriteria criteria) {
        if (criteria.getQuery() != null) {
            final String q = "%" + escapeLike(criteria.getQuery()) + "%";
            sql.append("AND (LOWER(cb.name) LIKE LOWER(:search) ESCAPE '\\' "
                    + "OR LOWER(cm.name) LIKE LOWER(:search) ESCAPE '\\' "
                    + "OR LOWER(COALESCE(c.description, '')) LIKE LOWER(:search) ESCAPE '\\' "
                    + "OR LOWER(CONCAT(COALESCE(la.start_point_street, ''), ' ', COALESCE(la.start_point_number, ''))) "
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
            sql.append("AND la.neighborhood_id IN (:searchNeighborhoodIds) ");
            params.put("searchNeighborhoodIds", criteria.getNeighborhoodIds());
        }
        if (!criteria.getCarTypes().isEmpty()) {
            sql.append("AND cm.type IN (:carTypes) ");
            params.put("carTypes", criteria.getCarTypes());
        }
        if (criteria.getMinPrice() != null) {
            sql.append("AND la.day_price >= :minPrice ");
            params.put("minPrice", criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() != null) {
            sql.append("AND la.day_price <= :maxPrice ");
            params.put("maxPrice", criteria.getMaxPrice());
        }
        appendCarSearchRatingBandFilter(sql, criteria.getRatingBands());
        if (criteria.isFlexibleSearch()) {
            appendFlexibleSearchFilter(sql, params, criteria);
        } else if (criteria.hasAvailabilityRange()) {
            final LocalDate searchFromWallDate = criteria.getAvailabilityRangeStart().atZone(WALL_ZONE).toLocalDate();
            final LocalDate searchUntilWallInclusive = criteria.getAvailabilityRangeEndExclusive()
                    .atZone(WALL_ZONE)
                    .toLocalDate()
                    .minusDays(1);
            final long rangeDays = criteria.getRangeLengthDays();
            if (rangeDays > 0) {
                sql.append("AND c.minimum_rental_days <= :rangeLengthDays ");
                params.put("rangeLengthDays", (int) rangeDays);
            }
            sql.append("AND EXISTS (")
                    .append("SELECT 1 FROM car_availability la_cover ")
                    .append("WHERE la_cover.car_id = c.id ")
                    .append("AND la_cover.kind = '").append(KIND_OFFERED).append("' ")
                    .append("AND la_cover.start_date <= :searchFromWallDate ")
                    .append("AND la_cover.end_date >= :searchUntilWallInclusive) ");
            sql.append("AND NOT EXISTS (")
                    .append("SELECT 1 FROM reservations r WHERE r.car_id = c.id ")
                    .append("AND r.status IN (").append(ACTIVE_RESERVATION_STATUS_CSV).append(") ")
                    .append("AND r.start_date < :resWindowEnd AND r.end_date > :resWindowStart) ");
            params.put("searchFromWallDate", java.sql.Date.valueOf(searchFromWallDate));
            params.put("searchUntilWallInclusive", java.sql.Date.valueOf(searchUntilWallInclusive));
            params.put("resWindowEnd", Timestamp.from(criteria.getAvailabilityRangeEndExclusive()));
            params.put("resWindowStart", Timestamp.from(criteria.getAvailabilityRangeStart()));
        }
    }

    private static void appendFlexibleSearchFilter(
            final StringBuilder sql, final Map<String, Object> params,
            final CarSearchCriteria criteria) {
        final YearMonth month = criteria.getFlexibleMonth();
        final LocalDate monthStart = month.atDay(1);
        final LocalDate monthEnd = month.atEndOfMonth();
        final Integer flexDays = criteria.getFlexibleDays();
        if (flexDays == null) {
            sql.append("AND EXISTS (")
                    .append("SELECT 1 FROM car_availability la_month ")
                    .append("WHERE la_month.car_id = c.id ")
                    .append("AND la_month.kind = '").append(KIND_OFFERED).append("' ")
                    .append("AND la_month.start_date <= :flexMonthEnd ")
                    .append("AND la_month.end_date >= :flexMonthStart) ");
            params.put("flexMonthStart", java.sql.Date.valueOf(monthStart));
            params.put("flexMonthEnd", java.sql.Date.valueOf(monthEnd));
        } else {
            sql.append("AND c.minimum_rental_days <= :flexibleDays ");
            sql.append("AND EXISTS (")
                    .append("SELECT 1 FROM car_availability la ")
                    .append("CROSS JOIN LATERAL generate_series(")
                    .append("    GREATEST(la.start_date, :flexMonthStart),")
                    .append("    LEAST(la.end_date, :flexMonthEnd) - (:flexibleDays - 1),")
                    .append("    INTERVAL '1 day'")
                    .append(") AS w(window_start) ")
                    .append("WHERE la.car_id = c.id ")
                    .append("AND la.kind = '").append(KIND_OFFERED).append("' ")
                    .append("AND la.start_date <= :flexMonthEnd ")
                    .append("AND la.end_date >= :flexMonthStart ")
                    .append("AND NOT EXISTS (")
                    .append("    SELECT 1 FROM reservations r ")
                    .append("    WHERE r.car_id = c.id ")
                    .append("    AND r.status IN (").append(ACTIVE_RESERVATION_STATUS_CSV).append(") ")
                    .append("    AND r.start_date < ((w.window_start + (:flexibleDays * INTERVAL '1 day')) AT TIME ZONE :wallZone) ")
                    .append("    AND r.end_date > (w.window_start AT TIME ZONE :wallZone)")
                    .append(")) ");
            params.put("flexMonthStart", java.sql.Date.valueOf(monthStart));
            params.put("flexMonthEnd", java.sql.Date.valueOf(monthEnd));
            params.put("flexibleDays", flexDays);
            params.put("wallZone", AppTimezone.ID);
        }
    }

    private static void appendCarSearchRatingBandFilter(
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

    private static String buildCarBrowseOrderBy(final String sortBy, final String sortDirection) {
        final String col = CAR_BROWSE_SORT_COLUMNS.getOrDefault(sortBy, "c.created_at");
        final String dir = "asc".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        if ("rating".equals(sortBy)) {
            return col + " " + dir + " NULLS LAST, c.created_at DESC, c.id ASC";
        }
        return col + " " + dir + ", c.id ASC";
    }

    private static BigDecimal toBigDecimal(final Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }
}
