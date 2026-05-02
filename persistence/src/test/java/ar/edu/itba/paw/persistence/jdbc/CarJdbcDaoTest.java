package ar.edu.itba.paw.persistence.jdbc;

import ar.edu.itba.paw.persistence.DaoIntegrationTestSupport;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Listing;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class CarJdbcDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private CarJdbcDao carDao;

    @Test
    public void testCreateCarPersistsRow() {
        // Arrange
        insertUser(1L, "owner@mail.com", "Owner", "One");

        // Exercise
        final Car created = carDao.createCar(
                1L,
                "AA123AA",
                "Toyota",
                "Corolla",
                Car.Type.SEDAN,
                Car.Powertrain.GASOLINE,
                Car.Transmission.AUTOMATIC);

        // Assert
        Assertions.assertTrue(created.getId() > 0);
        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT owner_id, plate, brand, model, type, transmission, powertrain FROM cars WHERE id = ?",
                created.getId());
        Assertions.assertEquals(1L, ((Number) row.get("OWNER_ID")).longValue());
        Assertions.assertEquals("AA123AA", row.get("PLATE"));
        Assertions.assertEquals("Toyota", row.get("BRAND"));
        Assertions.assertEquals("Corolla", row.get("MODEL"));
        Assertions.assertEquals("SEDAN", row.get("TYPE"));
        Assertions.assertEquals("AUTOMATIC", row.get("TRANSMISSION"));
        Assertions.assertEquals("GASOLINE", row.get("POWERTRAIN"));
    }

    @Test
    public void testGetCarByIdWhenNotFound() {


        // Exercise & Assert
        Assertions.assertTrue(carDao.getCarById(999L).isEmpty());
    }

    @Test
    public void testGetCheapestCarsReturnsOnlyActiveCarsSortedByPrice() {
        // Arrange
        final OffsetDateTime now = OffsetDateTime.parse("2026-04-01T10:00:00Z");
        insertUser(1L, "u1@mail.com", "A", "A");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertCar(11L, 1L, "P2", "VW", "Golf", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertCar(12L, 1L, "P3", "BMW", "X1", Car.Type.SUV, Car.Powertrain.DIESEL, Car.Transmission.AUTOMATIC);
        insertListing(100L, 10L, "A", Listing.Status.ACTIVE, new BigDecimal("70.00"), now);
        insertListing(101L, 11L, "B", Listing.Status.PAUSED, new BigDecimal("10.00"), now.plusMinutes(1));
        insertListing(102L, 12L, "C", Listing.Status.ACTIVE, new BigDecimal("40.00"), now.plusMinutes(2));

        // Exercise
        final List<Car> cars = carDao.getCheapestCars();

        // Assert
        Assertions.assertEquals(2, cars.size());
        Assertions.assertEquals(12L, cars.get(0).getId());
        Assertions.assertEquals(10L, cars.get(1).getId());
    }

    @Test
    public void testGetMostRecentCarsReturnsOnlyActiveCarsSortedByCreatedAt() {
        // Arrange
        final OffsetDateTime now = OffsetDateTime.parse("2026-04-01T10:00:00Z");
        insertUser(1L, "u1@mail.com", "A", "A");
        insertCar(20L, 1L, "P20", "Ford", "Focus", Car.Type.SEDAN, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertCar(21L, 1L, "P21", "VW", "Polo", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertCar(22L, 1L, "P22", "BMW", "X3", Car.Type.SUV, Car.Powertrain.DIESEL, Car.Transmission.AUTOMATIC);
        insertListing(200L, 20L, "L1", Listing.Status.ACTIVE, new BigDecimal("30.00"), now);
        insertListing(201L, 21L, "L2", Listing.Status.FINISHED, new BigDecimal("40.00"), now.plusDays(1));
        insertListing(202L, 22L, "L3", Listing.Status.ACTIVE, new BigDecimal("50.00"), now.plusDays(2));

        // Exercise
        final List<Car> cars = carDao.getMostRecentCars();

        // Assert
        Assertions.assertEquals(2, cars.size());
        Assertions.assertEquals(22L, cars.get(0).getId());
        Assertions.assertEquals(20L, cars.get(1).getId());
    }
}

