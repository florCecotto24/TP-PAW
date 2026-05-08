package ar.edu.itba.paw.persistence.jdbc;

import ar.edu.itba.paw.persistence.CarPictureDao;
import ar.edu.itba.paw.persistence.DaoIntegrationTestSupport;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarPicture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
public class CarPictureJdbcDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private CarPictureDao carPictureDao;

    @Test
    public void testCreateCarPicturePersistsRow() {
        // Arrange
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertImage(30L, "a.png", "image/png", new byte[] {1});

        // Exercise
        final CarPicture created = carPictureDao.createCarPicture(10L, 30L, 0);

        // Assert 
        Assertions.assertTrue(created.getId() > 0);
        Assertions.assertEquals(10L, created.getCarId());
        Assertions.assertEquals(30L, created.getImageId());
        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT car_id, image_id, display_order, created_at FROM car_pictures WHERE id = ?",
                created.getId());
        Assertions.assertEquals(10L, ((Number) row.get("CAR_ID")).longValue());
        Assertions.assertEquals(30L, ((Number) row.get("IMAGE_ID")).longValue());
        Assertions.assertEquals(0, ((Number) row.get("DISPLAY_ORDER")).intValue());
        Assertions.assertNotNull(row.get("CREATED_AT"));
    }

    @Test
    public void testGetCarPicturesByCarIdReturnsSortedByDisplayOrder() {
        // Arrange
        final OffsetDateTime now = OffsetDateTime.parse("2026-04-01T10:00:00Z");
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertImage(1L, "1.png", "image/png", new byte[] {1});
        insertImage(2L, "2.png", "image/png", new byte[] {2});
        insertCarPicture(101L, 10L, 2L, 2, now, now);
        insertCarPicture(102L, 10L, 1L, 1, now, now);

        // Exercise
        final List<CarPicture> pictures = carPictureDao.getCarPicturesByCarId(10L);

        // Assert: JDBC ordering is ground truth; DAO result must match.
        final List<Integer> orderFromDb = jdbcTemplate.query(
                "SELECT display_order FROM car_pictures WHERE car_id = ? ORDER BY display_order ASC",
                (rs, rn) -> rs.getInt(1),
                10L);
        Assertions.assertEquals(List.of(1, 2), orderFromDb);
        Assertions.assertEquals(
                orderFromDb,
                pictures.stream().map(CarPicture::getDisplayOrder).toList());
    }

    @Test
    public void testGetCarPictureByIdWhenNotFound() {


        // Exercise & Assert
        Assertions.assertTrue(carPictureDao.getCarPictureById(777L).isEmpty());
    }

    @Test
    public void testCreateCarPictureDuplicateDisplayOrderFailsByUniqueConstraint() {

        //
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertImage(1L, "1.png", "image/png", new byte[] {1});
        insertImage(2L, "2.png", "image/png", new byte[] {2});
        final OffsetDateTime t = OffsetDateTime.parse("2026-04-01T10:00:00Z");
        insertCarPicture(100L, 10L, 1L, 1, t, t);

        Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> carPictureDao.createCarPicture(10L, 2L, 1));
    }
}

