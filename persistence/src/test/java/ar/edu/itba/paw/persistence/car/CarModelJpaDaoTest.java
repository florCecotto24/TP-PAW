package ar.edu.itba.paw.persistence.car;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.persistence.car.CarModelDao;
import ar.edu.itba.paw.persistence.support.DaoIntegrationTestSupport;

class CarModelJpaDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private CarModelDao dao;

    @PersistenceContext
    private EntityManager em;

    private long brandId;
    private long otherBrandId;

    @BeforeEach
    void seedCatalog() {
        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "Honda", true);
        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "Toyota", true);
        brandId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_brands WHERE name = ?", Long.class, "Honda");
        otherBrandId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_brands WHERE name = ?", Long.class, "Toyota");
    }

    @Test
    void testCreatePersistsModelWithBrandValidatedAndType() {
        // 1. Arrange — only the parent brand is seeded by @BeforeEach.

        // 2. Act
        final CarModel created = dao.create(brandId, "Civic", false, Car.Type.SEDAN);
        em.flush();

        // 3. Assert — verify state with JdbcTemplate, not by re-reading via the DAO.
        final Long persistedBrandId = jdbcTemplate.queryForObject(
                "SELECT brand_id FROM car_models WHERE id = ?", Long.class, created.getId());
        final String name = jdbcTemplate.queryForObject(
                "SELECT name FROM car_models WHERE id = ?", String.class, created.getId());
        final Boolean validated = jdbcTemplate.queryForObject(
                "SELECT validated FROM car_models WHERE id = ?", Boolean.class, created.getId());
        final String type = jdbcTemplate.queryForObject(
                "SELECT type FROM car_models WHERE id = ?", String.class, created.getId());
        Assertions.assertEquals(brandId, persistedBrandId.longValue());
        Assertions.assertEquals("Civic", name);
        Assertions.assertFalse(validated);
        Assertions.assertEquals("SEDAN", type);
    }

    @Test
    void testFindByBrandIdOrderedReturnsValidatedFirstThenAlphabetical() {
        // 1. Arrange
        jdbcTemplate.update(
                "INSERT INTO car_models (brand_id, name, validated, type) VALUES (?, ?, ?, ?)",
                brandId, "zeta", true, Car.Type.SEDAN.name());
        jdbcTemplate.update(
                "INSERT INTO car_models (brand_id, name, validated, type) VALUES (?, ?, ?, ?)",
                brandId, "Alpha", false, Car.Type.SUV.name());
        jdbcTemplate.update(
                "INSERT INTO car_models (brand_id, name, validated, type) VALUES (?, ?, ?, ?)",
                brandId, "Beta", true, Car.Type.SEDAN.name());

        // 2. Act
        final List<CarModel> result = dao.findByBrandIdOrdered(brandId);

        // 3. Assert
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("Beta", result.get(0).getName());
        Assertions.assertEquals("zeta", result.get(1).getName());
        Assertions.assertEquals("Alpha", result.get(2).getName());
    }

    @Test
    void testFindByBrandIdAndNameIgnoreCaseMatchesScopedToBrand() {
        // 1. Arrange
        jdbcTemplate.update(
                "INSERT INTO car_models (brand_id, name, validated, type) VALUES (?, ?, ?, ?)",
                brandId, "Civic", true, Car.Type.SEDAN.name());
        jdbcTemplate.update(
                "INSERT INTO car_models (brand_id, name, validated, type) VALUES (?, ?, ?, ?)",
                otherBrandId, "Corolla", true, Car.Type.SEDAN.name());

        // 2. Act
        final Optional<CarModel> match = dao.findByBrandIdAndNameIgnoreCase(brandId, "CIVIC");
        final Optional<CarModel> wrongBrand = dao.findByBrandIdAndNameIgnoreCase(brandId, "Corolla");

        // 3. Assert
        Assertions.assertTrue(match.isPresent());
        Assertions.assertEquals("Civic", match.get().getName());
        Assertions.assertTrue(wrongBrand.isEmpty());
    }
}
