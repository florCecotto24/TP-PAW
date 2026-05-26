package ar.edu.itba.paw.persistence.hibernate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.dto.CarPriceMarketInsight;
import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.util.CarSearchCriteria;
import ar.edu.itba.paw.persistence.CarDao;
import ar.edu.itba.paw.persistence.DaoIntegrationTestSupport;

/** Phase 1 coverage: Car JPA mapping for the new status / description / timestamps / rating columns. */
class CarJpaDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private CarDao dao;

    @PersistenceContext
    private EntityManager em;

    private long ownerId;

    @BeforeEach
    void seedOwner() {
        jdbcTemplate.update("DELETE FROM listing_availability");
        jdbcTemplate.update("DELETE FROM cars");
        jdbcTemplate.update("DELETE FROM car_models");
        jdbcTemplate.update("DELETE FROM car_brands");
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", "car-owner@test.com");
        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "car-owner@test.com", "Owner", "Test");
        ownerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, "car-owner@test.com");
    }

    @Test
    void testCreateCarPersistsStatusActiveAndTimestamps() {
        // 1. Arrange — seed a brand and model so createCar has a valid carModelId
        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "Toyota", true);
        final Long brandId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_brands WHERE name = ?", Long.class, "Toyota");
        jdbcTemplate.update(
                "INSERT INTO car_models (brand_id, name, validated, type) VALUES (?, ?, ?, ?)",
                brandId, "Yaris", true, "HATCHBACK");
        final Long carModelId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_models WHERE name = ?", Long.class, "Yaris");

        // 2. Act
        final Car created = dao.createCar(
                ownerId, "AAA111", carModelId,
                Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        em.flush();

        // 3. Assert — verify state with JdbcTemplate, not by re-reading via the DAO.
        final String status = jdbcTemplate.queryForObject(
                "SELECT status FROM cars WHERE id = ?", String.class, created.getId());
        final OffsetDateTime createdAt = jdbcTemplate.queryForObject(
                "SELECT created_at FROM cars WHERE id = ?", OffsetDateTime.class, created.getId());
        final OffsetDateTime updatedAt = jdbcTemplate.queryForObject(
                "SELECT updated_at FROM cars WHERE id = ?", OffsetDateTime.class, created.getId());
        Assertions.assertEquals("active", status);
        Assertions.assertNotNull(createdAt);
        Assertions.assertNotNull(updatedAt);
    }

    @Test
    void testEntityReadsStatusEnumFromLowercaseStringInDb() {
        // 1. Arrange — insert a car directly with a non-default status via JdbcTemplate (no DAO call).
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                "INSERT INTO cars (owner_id, plate, brand, model, type, transmission, powertrain, "
                        + "status, description, created_at, updated_at, rating_avg) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                ownerId, "BBB222", "Honda", "Civic",
                "SEDAN", "AUTOMATIC", "GASOLINE",
                "admin_paused", "Some description", now, now, new BigDecimal("4.50"));
        final long carId = jdbcTemplate.queryForObject(
                "SELECT id FROM cars WHERE plate = ?", Long.class, "BBB222");

        // 2. Act
        final Optional<Car> found = dao.getCarById(carId);

        // 3. Assert
        Assertions.assertTrue(found.isPresent());
        final Car car = found.get();
        Assertions.assertEquals(Car.Status.ADMIN_PAUSED, car.getStatus());
        Assertions.assertEquals(Optional.of("Some description"), car.getDescription());
        Assertions.assertEquals(0, new BigDecimal("4.50").compareTo(car.getRatingAvg().orElseThrow()));
    }

    @Test
    void testEntityMutationFlushesStatusToLowercaseInDb() {
        // 1. Arrange — seed via JdbcTemplate, then load through the EntityManager (not the DAO under test).
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                "INSERT INTO cars (owner_id, plate, brand, model, type, transmission, powertrain, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                ownerId, "CCC333", "Ford", "Focus", "HATCHBACK", "MANUAL", "GASOLINE",
                "active", now, now);
        final long carId = jdbcTemplate.queryForObject(
                "SELECT id FROM cars WHERE plate = ?", Long.class, "CCC333");
        final Car managed = em.find(Car.class, carId);

        // 2. Act
        managed.setStatus(Car.Status.LACK_DOC);
        em.flush();

        // 3. Assert
        final String status = jdbcTemplate.queryForObject(
                "SELECT status FROM cars WHERE id = ?", String.class, carId);
        Assertions.assertEquals("lack_doc", status);
    }

    @Test
    void testFindActiveDayPriceMarketInsightAggregatesPerCarMinPriceAndExcludesCar() {
        // 1. Arrange — catalog + three active cars (two Corolla peers, one Ford Ka outlier).
        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "Toyota", true);
        final long toyotaBrandId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_brands WHERE name = ?", Long.class, "Toyota");
        jdbcTemplate.update(
                "INSERT INTO car_models (brand_id, name, validated, type) VALUES (?, ?, ?, ?)",
                toyotaBrandId, "Corolla", true, "SEDAN");
        final long corollaModelId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_models WHERE name = ?", Long.class, "Corolla");

        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "Ford", true);
        final long fordBrandId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_brands WHERE name = ?", Long.class, "Ford");
        jdbcTemplate.update(
                "INSERT INTO car_models (brand_id, name, validated, type) VALUES (?, ?, ?, ?)",
                fordBrandId, "Ka", true, "HATCHBACK");
        final long kaModelId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_models WHERE name = ?", Long.class, "Ka");

        final Car corollaA = dao.createCar(
                ownerId, "COR001", corollaModelId,
                Car.Type.SEDAN, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        final Car corollaB = dao.createCar(
                ownerId, "COR002", corollaModelId,
                Car.Type.SEDAN, Car.Powertrain.GASOLINE, Car.Transmission.AUTOMATIC);
        final Car ka = dao.createCar(
                ownerId, "KA0001", kaModelId,
                Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        em.flush();

        final OffsetDateTime t = OffsetDateTime.parse("2026-07-01T10:00:00Z");
        insertOfferedAvailability(corollaA.getId(), new BigDecimal("80.00"), t);
        insertOfferedAvailability(corollaA.getId(), new BigDecimal("50.00"), t.plusDays(1));
        insertOfferedAvailability(corollaB.getId(), new BigDecimal("120.00"), t);
        insertOfferedAvailability(ka.getId(), new BigDecimal("200.00"), t);

        // 2. Act — market stats for Toyota Corolla (two cars, min prices 50 and 120).
        final Optional<CarPriceMarketInsight> market = dao.findActiveDayPriceMarketInsightByBrandAndModel(
                "Toyota", "Corolla", null);
        final Optional<CarPriceMarketInsight> excludingCorollaA = dao.findActiveDayPriceMarketInsightByBrandAndModel(
                "Toyota", "Corolla", corollaA.getId());

        // 3. Assert — per-car MIN aggregation (not raw segment count).
        Assertions.assertTrue(market.isPresent());
        final CarPriceMarketInsight insight = market.get();
        Assertions.assertEquals(0, new BigDecimal("50.00").compareTo(insight.getMinPrice()));
        Assertions.assertEquals(0, new BigDecimal("120.00").compareTo(insight.getMaxPrice()));
        Assertions.assertEquals(0, new BigDecimal("85.00").compareTo(insight.getAveragePrice()));
        Assertions.assertEquals(2L, insight.getSampleCount());

        Assertions.assertTrue(excludingCorollaA.isPresent());
        final CarPriceMarketInsight excluded = excludingCorollaA.get();
        Assertions.assertEquals(0, new BigDecimal("120.00").compareTo(excluded.getMinPrice()));
        Assertions.assertEquals(0, new BigDecimal("120.00").compareTo(excluded.getMaxPrice()));
        Assertions.assertEquals(0, new BigDecimal("120.00").compareTo(excluded.getAveragePrice()));
        Assertions.assertEquals(1L, excluded.getSampleCount());
    }

    @Test
    void testSearchCarCardsWithAvailabilityRangeReturnsOnlyCarsCoveringWholeWallRange() {
        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "Toyota", true);
        final long brandId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_brands WHERE name = ?", Long.class, "Toyota");
        jdbcTemplate.update(
                "INSERT INTO car_models (brand_id, name, validated, type) VALUES (?, ?, ?, ?)",
                brandId, "Corolla", true, "SEDAN");
        final long modelId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_models WHERE name = ?", Long.class, "Corolla");

        final Car outsideRange = dao.createCar(
                ownerId, "OUT111", modelId,
                Car.Type.SEDAN, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        final Car coversRange = dao.createCar(
                ownerId, "IN1111", modelId,
                Car.Type.SEDAN, Car.Powertrain.GASOLINE, Car.Transmission.AUTOMATIC);
        em.flush();

        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-01T10:00:00Z");
        insertOfferedAvailability(
                outsideRange.getId(),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 10),
                new BigDecimal("20.00"),
                createdAt);
        insertOfferedAvailability(
                coversRange.getId(),
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 6, 30),
                new BigDecimal("25.00"),
                createdAt.plusMinutes(1));

        final Instant rangeStart = LocalDate.of(2026, 6, 15)
                .atStartOfDay(AvailabilityPeriod.WALL_ZONE)
                .toInstant();
        final Instant rangeEndExclusive = LocalDate.of(2026, 6, 19)
                .atStartOfDay(AvailabilityPeriod.WALL_ZONE)
                .toInstant();
        final CarSearchCriteria criteria = CarSearchCriteria.builder()
                .availabilityRange(rangeStart, rangeEndExclusive)
                .browseWallDate(LocalDate.of(2026, 6, 1))
                .page(0)
                .uiPageSize(10)
                .dbFetchSize(10)
                .sortBy("date")
                .sortDirection("desc")
                .build();

        final Page<CarCard> result = dao.searchCarCards(criteria);

        Assertions.assertEquals(1, result.getContent().size());
        Assertions.assertEquals(coversRange.getId(), result.getContent().get(0).getCarId());
        Assertions.assertEquals(1L, result.getTotalItems());
    }

    private void insertOfferedAvailability(
            final long carId,
            final BigDecimal dayPrice,
            final OffsetDateTime createdAt) {
        insertOfferedAvailability(
                carId,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                dayPrice,
                createdAt);
    }

    private void insertOfferedAvailability(
            final long carId,
            final LocalDate startDate,
            final LocalDate endDate,
            final BigDecimal dayPrice,
            final OffsetDateTime createdAt) {
        jdbcTemplate.update(
                "INSERT INTO listing_availability (car_id, start_date, end_date, day_price, "
                        + "start_point_street, check_in_time, check_out_time, kind, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                carId,
                startDate,
                endDate,
                dayPrice,
                "Belgrano",
                LocalTime.of(10, 0),
                LocalTime.of(18, 0),
                "offered",
                createdAt,
                createdAt);
    }
}
