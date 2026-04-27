package ar.edu.itba.paw.persistence.jdbc;

import ar.edu.itba.paw.persistence.DaoIntegrationTestSupport;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarPicture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public class CarPictureJdbcDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private CarPictureJdbcDao carPictureDao;

    @Test
    public void testCreateAndGetCarPictureById() {
        // Arrange
        final OffsetDateTime now = OffsetDateTime.parse("2026-04-01T10:00:00Z");
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertImage(30L, "a.png", "image/png", new byte[] {1});

        // Exercise
        final CarPicture created = carPictureDao.createCarPicture(10L, 30L, 0);
        final Optional<CarPicture> found = carPictureDao.getCarPictureById(created.getId());

        // Assert
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(10L, found.get().getCarId());
        Assertions.assertEquals(30L, found.get().getImageId());
        Assertions.assertTrue(found.get().getCreatedAt().isAfter(now.minusDays(1)));
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

        // Assert
        Assertions.assertEquals(2, pictures.size());
        Assertions.assertEquals(1, pictures.get(0).getDisplayOrder());
        Assertions.assertEquals(2, pictures.get(1).getDisplayOrder());
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
        carPictureDao.createCarPicture(10L, 1L, 1);

        Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> carPictureDao.createCarPicture(10L, 2L, 1));
    }
}

