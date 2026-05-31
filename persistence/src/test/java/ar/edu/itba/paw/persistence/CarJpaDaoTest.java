package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.dto.car.CarPriceMarketInsight;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.models.util.search.CarSearchCriteria;
import ar.edu.itba.paw.models.util.search.OwnerCarSearchCriteria;
import ar.edu.itba.paw.persistence.CarDao;
import ar.edu.itba.paw.persistence.support.DaoIntegrationTestSupport;

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
                2020, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
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
                "INSERT INTO cars (owner_id, plate, transmission, powertrain, "
                        + "status, description, created_at, updated_at, rating_avg) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                ownerId, "BBB222",
                "AUTOMATIC", "GASOLINE",
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
                "INSERT INTO cars (owner_id, plate, transmission, powertrain, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                ownerId, "CCC333", "MANUAL", "GASOLINE",
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
                2020, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        final Car corollaB = dao.createCar(
                ownerId, "COR002", corollaModelId,
                2020, Car.Powertrain.GASOLINE, Car.Transmission.AUTOMATIC);
        final Car ka = dao.createCar(
                ownerId, "KA0001", kaModelId,
                2018, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
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
    void testUpdateMinimumRentalDaysPersistsValueViaJdbcTemplate() {
        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "Honda", true);
        final long brandId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_brands WHERE name = ?", Long.class, "Honda");
        jdbcTemplate.update(
                "INSERT INTO car_models (brand_id, name, validated, type) VALUES (?, ?, ?, ?)",
                brandId, "Civic", true, "SEDAN");
        final long modelId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_models WHERE name = ?", Long.class, "Civic");

        final Car car = dao.createCar(
                ownerId, "HON001", modelId,
                2021, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        em.flush();

        dao.updateMinimumRentalDays(car.getId(), 3);
        em.flush();

        final int persisted = jdbcTemplate.queryForObject(
                "SELECT minimum_rental_days FROM cars WHERE id = ?", Integer.class, car.getId());
        Assertions.assertEquals(3, persisted);
    }

    @Test
    void testSearchCarCardsExcludesCarsWhoseMinimumRentalDaysExceedsRangeLength() {
        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "Hyundai", true);
        final long brandId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_brands WHERE name = ?", Long.class, "Hyundai");
        jdbcTemplate.update(
                "INSERT INTO car_models (brand_id, name, validated, type) VALUES (?, ?, ?, ?)",
                brandId, "Elantra", true, "SEDAN");
        final long modelId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_models WHERE name = ?", Long.class, "Elantra");

        final Car fitsMin = dao.createCar(
                ownerId, "HYU001", modelId,
                2022, Car.Powertrain.GASOLINE, Car.Transmission.AUTOMATIC);
        final Car exceedsMin = dao.createCar(
                ownerId, "HYU002", modelId,
                2022, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        em.flush();

        final OffsetDateTime createdAt = OffsetDateTime.parse("2030-01-01T00:00:00Z");
        insertOfferedAvailability(fitsMin.getId(), LocalDate.of(2030, 6, 1), LocalDate.of(2030, 6, 30),
                new BigDecimal("50.00"), createdAt);
        insertOfferedAvailability(exceedsMin.getId(), LocalDate.of(2030, 6, 1), LocalDate.of(2030, 6, 30),
                new BigDecimal("50.00"), createdAt);

        jdbcTemplate.update("UPDATE cars SET minimum_rental_days = ? WHERE id = ?", 1, fitsMin.getId());
        jdbcTemplate.update("UPDATE cars SET minimum_rental_days = ? WHERE id = ?", 5, exceedsMin.getId());

        // Search range of 1 day: only fitsMin (minDays=1) should appear; exceedsMin (minDays=5) excluded.
        final Instant rangeStart = LocalDate.of(2030, 6, 10)
                .atStartOfDay(AppTimezone.WALL_ZONE).toInstant();
        final Instant rangeEndExclusive = LocalDate.of(2030, 6, 11)
                .atStartOfDay(AppTimezone.WALL_ZONE).toInstant();
        final CarSearchCriteria criteria = CarSearchCriteria.builder()
                .availabilityRange(rangeStart, rangeEndExclusive)
                .browseWallDate(LocalDate.of(2030, 6, 1))
                .page(0)
                .uiPageSize(10)
                .dbFetchSize(10)
                .sortBy("date")
                .sortDirection("desc")
                .build();

        final Page<CarCard> result = dao.searchCarCards(criteria);

        final List<Long> ids = result.getContent().stream()
                .map(CarCard::getCarId)
                .collect(Collectors.toList());
        Assertions.assertTrue(ids.contains(fitsMin.getId()), "Car with minDays=1 should be included");
        Assertions.assertFalse(ids.contains(exceedsMin.getId()), "Car with minDays=5 should be excluded for 1-day range");
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
                2020, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        final Car coversRange = dao.createCar(
                ownerId, "IN1111", modelId,
                2020, Car.Powertrain.GASOLINE, Car.Transmission.AUTOMATIC);
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
                .atStartOfDay(AppTimezone.WALL_ZONE)
                .toInstant();
        final Instant rangeEndExclusive = LocalDate.of(2026, 6, 19)
                .atStartOfDay(AppTimezone.WALL_ZONE)
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

    @Test
    void testGetOwnerCarCardsUsesFirstImageSkippingLeadingVideo() {
        // 1. Arrange
        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "Toyota", true);
        final long brandId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_brands WHERE name = ?", Long.class, "Toyota");
        jdbcTemplate.update(
                "INSERT INTO car_models (brand_id, name, validated, type) VALUES (?, ?, ?, ?)",
                brandId, "Yaris", true, "HATCHBACK");
        final long modelId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_models WHERE name = ?", Long.class, "Yaris");

        final Car car = dao.createCar(
                ownerId, "VID111", modelId,
                2020, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        em.flush();

        jdbcTemplate.update(
                "INSERT INTO images (image_name, content_type, byte_array) VALUES (?, ?, ?)",
                "cover.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3});
        final long imageId = jdbcTemplate.queryForObject(
                "SELECT id FROM images WHERE image_name = ?", Long.class, "cover.jpg");

        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                "INSERT INTO stored_files (uploader_user_id, file_name, content_type, byte_array, created_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                ownerId,
                "intro.mp4",
                "video/mp4",
                new byte[] {4, 5, 6},
                now);
        final long storedFileId = jdbcTemplate.queryForObject(
                "SELECT id FROM stored_files WHERE file_name = ?", Long.class, "intro.mp4");

        jdbcTemplate.update(
                "INSERT INTO car_pictures (car_id, image_id, stored_file_id, display_order, created_at, updated_at) "
                        + "VALUES (?, NULL, ?, 1, ?, ?)",
                car.getId(),
                storedFileId,
                now,
                now);
        jdbcTemplate.update(
                "INSERT INTO car_pictures (car_id, image_id, stored_file_id, display_order, created_at, updated_at) "
                        + "VALUES (?, ?, NULL, 2, ?, ?)",
                car.getId(),
                imageId,
                now,
                now);

        insertOfferedAvailability(car.getId(), new BigDecimal("30.00"), now);

        final OwnerCarSearchCriteria criteria = new OwnerCarSearchCriteria(
                ownerId, 0, 10,
                List.of("active"), null, null, null, null, null, null, null,
                "date", "desc", null);

        // 2. Exercise
        final Page<CarCard> result = dao.getOwnerCarCards(criteria);

        // 3. Assert
        final Optional<CarCard> card = result.getContent().stream()
                .filter(c -> c.getCarId() == car.getId())
                .findFirst();
        Assertions.assertTrue(card.isPresent());
        Assertions.assertEquals(imageId, card.get().getImageId());
    }

    @Test
    void testSearchCarCardsAndCountExcludeCarsOfBlockedOwners() {
        // 1. Arrange — two owners, two cars in the same brand/model and same availability period.
        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since, blocked) VALUES (?, ?, ?, CURRENT_DATE, ?)",
                "blocked-owner@test.com", "Bad", "Owner", true);
        final long blockedOwnerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, "blocked-owner@test.com");

        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "Volkswagen", true);
        final long brandId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_brands WHERE name = ?", Long.class, "Volkswagen");
        jdbcTemplate.update(
                "INSERT INTO car_models (brand_id, name, validated, type) VALUES (?, ?, ?, ?)",
                brandId, "Gol", true, "HATCHBACK");
        final long modelId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_models WHERE name = ?", Long.class, "Gol");

        final Car visibleCar = dao.createCar(
                ownerId, "VIS001", modelId,
                2022, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        final Car hiddenCar = dao.createCar(
                blockedOwnerId, "HID001", modelId,
                2022, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        em.flush();

        final OffsetDateTime createdAt = OffsetDateTime.parse("2030-01-01T00:00:00Z");
        insertOfferedAvailability(visibleCar.getId(), LocalDate.of(2030, 6, 1), LocalDate.of(2030, 6, 30),
                new BigDecimal("40.00"), createdAt);
        insertOfferedAvailability(hiddenCar.getId(), LocalDate.of(2030, 6, 1), LocalDate.of(2030, 6, 30),
                new BigDecimal("40.00"), createdAt);

        final CarSearchCriteria criteria = CarSearchCriteria.builder()
                .browseWallDate(LocalDate.of(2030, 6, 1))
                .page(0).uiPageSize(10).dbFetchSize(10)
                .sortBy("date").sortDirection("desc")
                .build();

        // 2. Act — exercise the public browse pagination API, which owns the COUNT internally.
        final Page<CarCard> page = dao.searchCarCards(criteria);
        final long browseCount = dao.getMostRecentCarCards(0, 10, LocalDate.of(2030, 6, 1), null).getTotalItems();

        // 3. Assert — only the car owned by the non-blocked owner is visible to consumers.
        final List<Long> visibleIds = page.getContent().stream().map(CarCard::getCarId).collect(Collectors.toList());
        Assertions.assertTrue(visibleIds.contains(visibleCar.getId()), "Car of non-blocked owner must be browse-visible");
        Assertions.assertFalse(visibleIds.contains(hiddenCar.getId()), "Car of blocked owner must be hidden from the catalog");
        Assertions.assertEquals(1L, browseCount, "Browse-page total must also exclude blocked-owner listings");
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
