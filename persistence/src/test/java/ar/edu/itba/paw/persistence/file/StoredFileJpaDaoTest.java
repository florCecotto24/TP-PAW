package ar.edu.itba.paw.persistence.file;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.persistence.support.DaoIntegrationTestSupport;

class StoredFileJpaDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private StoredFileDao dao;

    @PersistenceContext
    private EntityManager em;

    private long uploaderId;

    @BeforeEach
    void seedUploader() {
        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "uploader@test.com",
                "Up",
                "Loader");
        uploaderId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, "uploader@test.com");
    }

    @Test
    void testCreatePersistsUploaderMetadataAndBytes() {
        // 1. Arrange
        final byte[] payload = new byte[] {10, 11, 12};

        // 2. Act
        final StoredFile created = dao.create(uploaderId, "receipt.pdf", "application/pdf", payload);
        em.flush();

        // 3. Assert
        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT uploader_user_id, file_name, content_type, byte_array FROM stored_files WHERE id = ?",
                created.getId());
        Assertions.assertEquals(uploaderId, ((Number) row.get("uploader_user_id")).longValue());
        Assertions.assertEquals("receipt.pdf", row.get("file_name"));
        Assertions.assertEquals("application/pdf", row.get("content_type"));
        Assertions.assertTrue(Arrays.equals(payload, (byte[]) row.get("byte_array")));
    }

    @Test
    void testCreateSetsCreatedAt() {
        // 1. Arrange / 2. Act
        final StoredFile created = dao.create(uploaderId, "doc.pdf", "application/pdf", new byte[] {1});
        em.flush();

        // 3. Assert
        final OffsetDateTime createdAt = jdbcTemplate.queryForObject(
                "SELECT created_at FROM stored_files WHERE id = ?",
                OffsetDateTime.class,
                created.getId());
        Assertions.assertNotNull(createdAt);
        Assertions.assertTrue(createdAt.isBefore(OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(5)));
    }
}
