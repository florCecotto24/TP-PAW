package ar.edu.itba.paw.persistence.hibernate;

import java.math.BigDecimal;
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
        jdbcTemplate.update("DELETE FROM cars");
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
}
