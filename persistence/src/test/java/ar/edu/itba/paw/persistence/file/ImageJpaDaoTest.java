package ar.edu.itba.paw.persistence.file;

import java.util.Arrays;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.models.domain.file.Image;
import ar.edu.itba.paw.persistence.support.DaoIntegrationTestSupport;

class ImageJpaDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private ImageDao dao;

    @PersistenceContext
    private EntityManager em;

    @Test
    void testCreateImagePersistsMetadataAndBytes() {
        // 1. Arrange
        final byte[] payload = new byte[] {9, 8, 7};

        // 2. Act
        final Image created = dao.createImage("avatar.png", "image/png", payload);
        em.flush();

        // 3. Assert
        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT image_name, content_type, byte_array FROM images WHERE id = ?",
                created.getId());
        Assertions.assertEquals("avatar.png", row.get("image_name"));
        Assertions.assertEquals("image/png", row.get("content_type"));
        Assertions.assertTrue(Arrays.equals(payload, (byte[]) row.get("byte_array")));
    }

    @Test
    void testDeleteImageRemovesRow() {
        // 1. Arrange
        final Image created = dao.createImage("temp.jpg", "image/jpeg", new byte[] {1});
        em.flush();
        final long imageId = created.getId();

        // 2. Act
        dao.deleteImage(imageId);
        em.flush();

        // 3. Assert
        Assertions.assertEquals(
                0,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM images WHERE id = ?", Integer.class, imageId));
    }
}
