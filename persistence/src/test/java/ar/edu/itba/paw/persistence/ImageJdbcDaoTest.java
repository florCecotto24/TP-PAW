package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Image;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

public class ImageJdbcDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private ImageJdbcDao imageDao;

    @Test
    public void testCreateAndGetImageById() {
        // Arrange
        final byte[] data = new byte[] {1, 2, 3, 4};

        // Exercise
        final Image created = imageDao.createImage("car.png", "image/png", data);
        final Optional<Image> found = imageDao.getImageById(created.getId());

        // Assert
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals("car.png", found.get().getName());
        Assertions.assertArrayEquals(data, found.get().getData());
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
}

