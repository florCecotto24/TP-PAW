package ar.edu.itba.paw.persistence.jdbc;

import ar.edu.itba.paw.persistence.DaoIntegrationTestSupport;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ar.edu.itba.paw.models.domain.Image;

public class ImageJdbcDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private ImageJdbcDao imageDao;

    @Test
    public void testCreateImagePersistsRow() {
        // Arrange
        final byte[] data = new byte[] {1, 2, 3, 4};

        // Exercise
        final Image created = imageDao.createImage("car.png", "image/png", data);

        // Assert
        Assertions.assertTrue(created.getId() > 0);
        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT image_name, content_type, byte_array FROM images WHERE id = ?",
                created.getId());
        Assertions.assertEquals("car.png", row.get("IMAGE_NAME"));
        Assertions.assertEquals("image/png", row.get("CONTENT_TYPE"));
        Assertions.assertArrayEquals(data, (byte[]) row.get("BYTE_ARRAY"));
    }

    @Test
    public void testGetImageByIdWhenNotFound() {
        // Arrange
        // (no special arrangement needed)

        // Exercise & Assert
        Assertions.assertTrue(imageDao.getImageById(999L).isEmpty());
    }

    @Test
    public void testCreateImageWithNullContentTypeFailsByConstraint() {


        // Exercise & Assert
        Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> imageDao.createImage("a.png", null, new byte[] {1}));
    }

    @Test
    public void testDeleteImageRemovesRow() {
        // Arrange — row from SQL, not createImage on the same DAO under test
        final long imageId = 77L;
        final byte[] data = new byte[] {9};
        insertImage(imageId, "x.png", "image/png", data);

        // Exercise
        imageDao.deleteImage(imageId);

        // Assert
        final Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM images WHERE id = ?", Integer.class, imageId);
        Assertions.assertEquals(Integer.valueOf(0), count);
    }
}

