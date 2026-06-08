package ar.edu.itba.paw.persistence.car;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.models.domain.car.CarBrand;
import ar.edu.itba.paw.persistence.car.CarBrandDao;
import ar.edu.itba.paw.persistence.support.DaoIntegrationTestSupport;

class CarBrandJpaDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private CarBrandDao dao;

    @PersistenceContext
    private EntityManager em;

    @Test
    void testCreatePersistsBrandWithGivenValidatedFlag() {
        // 1. Arrange — empty catalog (each test runs in its own rolled-back transaction).

        // 2. Act
        final CarBrand created = dao.create("Toyota", false);
        em.flush();

        // 3. Assert — verify state with JdbcTemplate, not by re-reading via the DAO.
        final String name = jdbcTemplate.queryForObject(
                "SELECT name FROM car_brands WHERE id = ?", String.class, created.getId());
        final Boolean validated = jdbcTemplate.queryForObject(
                "SELECT validated FROM car_brands WHERE id = ?", Boolean.class, created.getId());
        Assertions.assertEquals("Toyota", name);
        Assertions.assertFalse(validated);
    }

    @Test
    void testFindAllOrderedReturnsValidatedBrandsFirstThenAlphabetical() {
        // 1. Arrange — fixtures via JdbcTemplate, never via the DAO under test.
        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "zeta", true);
        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "Alpha", false);
        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "Beta", true);

        // 2. Act
        final List<CarBrand> result = dao.findAllOrdered();

        // 3. Assert
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("Beta", result.get(0).getName());
        Assertions.assertEquals("zeta", result.get(1).getName());
        Assertions.assertEquals("Alpha", result.get(2).getName());
    }

    @Test
    void testFindValidatedOrderedExcludesUnvalidatedBrands() {
        // 1. Arrange
        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "Honda", true);
        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "Otro", false);

        // 2. Act
        final List<CarBrand> result = dao.findValidatedOrdered();

        // 3. Assert
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Honda", result.get(0).getName());
    }

    @Test
    void testFindByNameIgnoreCaseMatchesRegardlessOfCasing() {
        // 1. Arrange
        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "Mercedes-Benz", true);

        // 2. Act
        final Optional<CarBrand> result = dao.findByNameIgnoreCase("mercedes-benz");

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("Mercedes-Benz", result.get().getName());
    }

    @Test
    void testFindByNameIgnoreCaseReturnsEmptyForBlankInput() {
        // 1. Arrange
        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "Honda", true);

        // 2. Act
        final Optional<CarBrand> blank = dao.findByNameIgnoreCase("   ");
        final Optional<CarBrand> nullArg = dao.findByNameIgnoreCase(null);

        // 3. Assert
        Assertions.assertTrue(blank.isEmpty());
        Assertions.assertTrue(nullArg.isEmpty());
    }
}
