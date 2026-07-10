package ar.edu.itba.paw.persistence.car;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.models.domain.car.CarPicture;
import ar.edu.itba.paw.persistence.car.CarPictureDao;
import ar.edu.itba.paw.persistence.support.DaoIntegrationTestSupport;

class CarPictureJpaDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private CarPictureDao dao;

    @PersistenceContext
    private EntityManager em;

    private long carId;
    private long imageId;
    private long storedFileId;

    @BeforeEach
    void seedCarAndMedia() {
        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "gallery-owner@test.com",
                "Owner",
                "Test");
        final long ownerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, "gallery-owner@test.com");

        jdbcTemplate.update(
                "INSERT INTO cars (owner_id, plate, transmission, powertrain) VALUES (?, ?, ?, ?)",
                ownerId,
                "GAL01",
                "manual",
                "gasoline");
        carId = jdbcTemplate.queryForObject("SELECT id FROM cars WHERE plate = ?", Long.class, "GAL01");

        jdbcTemplate.update(
                "INSERT INTO images (image_name, content_type, byte_array) VALUES (?, ?, ?)",
                "photo.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3});
        imageId = jdbcTemplate.queryForObject(
                "SELECT id FROM images WHERE image_name = ?", Long.class, "photo.jpg");

        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                "INSERT INTO stored_files (uploader_user_id, file_name, content_type, byte_array, created_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                ownerId,
                "clip.mp4",
                "video/mp4",
                new byte[] {4, 5, 6},
                now);
        storedFileId = jdbcTemplate.queryForObject(
                "SELECT id FROM stored_files WHERE file_name = ?", Long.class, "clip.mp4");
    }

    @Test
    void testCreateCarPicturePersistsImageReference() {
        // 1. Arrange / 2. Act
        final CarPicture created = dao.createCarPicture(carId, imageId, 1);
        em.flush();

        // 3. Assert
        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT image_id, stored_file_id, display_order FROM car_pictures WHERE id = ?",
                created.getId());
        Assertions.assertEquals(imageId, ((Number) row.get("image_id")).longValue());
        Assertions.assertNull(row.get("stored_file_id"));
        Assertions.assertEquals(1, ((Number) row.get("display_order")).intValue());
    }

    @Test
    void testCreateCarPictureFromVideoPersistsStoredFileReference() {
        // 1. Arrange / 2. Act
        final CarPicture created = dao.createCarPictureFromVideo(carId, storedFileId, 2);
        em.flush();

        // 3. Assert
        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT image_id, stored_file_id, display_order FROM car_pictures WHERE id = ?",
                created.getId());
        Assertions.assertNull(row.get("image_id"));
        Assertions.assertEquals(storedFileId, ((Number) row.get("stored_file_id")).longValue());
        Assertions.assertEquals(2, ((Number) row.get("display_order")).intValue());
    }

    @Test
    void testIsStoredFileInCarGalleryReturnsTrueWhenLinked() {
        // 1. Arrange — seed a gallery picture pointing at the stored file via JdbcTemplate.
        insertCarPictureFromVideo(carId, storedFileId, 1);

        // 2. Act / 3. Assert
        Assertions.assertTrue(dao.isStoredFileInCarGallery(storedFileId));
        Assertions.assertFalse(dao.isStoredFileInCarGallery(storedFileId + 9999));
    }

    @Test
    void testFindByCarIdOrderByDisplayOrderAscPaginatesInSql() {
        // 1. Arrange — seed three pictures
        insertCarPicture(imageId, 1);
        insertCarPicture(imageId, 2);
        insertCarPicture(imageId, 3);

        // 2. Act
        final List<CarPicture> pageOne = dao.findByCarIdOrderByDisplayOrderAsc(carId, 0, 2);
        final List<CarPicture> pageTwo = dao.findByCarIdOrderByDisplayOrderAsc(carId, 2, 2);

        // 3. Assert
        Assertions.assertEquals(2, pageOne.size());
        Assertions.assertEquals(1, pageTwo.size());
        Assertions.assertEquals(1, pageOne.get(0).getDisplayOrder());
        Assertions.assertEquals(2, pageOne.get(1).getDisplayOrder());
        Assertions.assertEquals(3, pageTwo.get(0).getDisplayOrder());
        Assertions.assertEquals(3L, dao.countByCarId(carId));
    }

    @Test
    void testFindFirstByCarIdOrderByDisplayOrderAscReturnsLowestDisplayOrder() {
        // 1. Arrange — seed two pictures
        insertCarPicture(imageId, 3);
        insertCarPicture(imageId, 1);

        // 2. Act
        final Optional<CarPicture> primary = dao.findFirstByCarIdOrderByDisplayOrderAsc(carId);

        // 3. Assert
        Assertions.assertTrue(primary.isPresent());
        Assertions.assertEquals(1, primary.get().getDisplayOrder());
    }

    @Test
    void testFindMaxDisplayOrderByCarIdReturnsHighestValue() {
        // 1. Arrange — seed two pictures
        insertCarPicture(imageId, 2);
        insertCarPicture(imageId, 5);

        // 2. Act / 3. Assert
        Assertions.assertEquals(Optional.of(5), dao.findMaxDisplayOrderByCarId(carId));
        Assertions.assertTrue(dao.findMaxDisplayOrderByCarId(carId + 9999).isEmpty());
    }

    private void insertCarPicture(final long imageId, final int displayOrder) {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                "INSERT INTO car_pictures (car_id, image_id, display_order, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                carId, imageId, displayOrder, now, now);
    }

    private void insertCarPictureFromVideo(final long carId, final long storedFileId, final int displayOrder) {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                "INSERT INTO car_pictures (car_id, stored_file_id, display_order, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                carId, storedFileId, displayOrder, now, now);
    }
}
