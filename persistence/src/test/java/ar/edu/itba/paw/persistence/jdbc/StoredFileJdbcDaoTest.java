package ar.edu.itba.paw.persistence.jdbc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.persistence.DaoIntegrationTestSupport;

class StoredFileJdbcDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private StoredFileJdbcDao dao;

    private static final long UPLOADER_ID = 1L;

    @Test
    void testCreatePersistsRowAndReturnsGeneratedId() {
        // 1.Arrange
        insertUser(UPLOADER_ID, "user@example.com", "Ada", "Lovelace");
        final byte[] data = new byte[]{1, 2, 3, 4, 5};

        // 2.Exercise
        final StoredFile created = dao.create(UPLOADER_ID, "doc.pdf", "application/pdf", data);

        // 3.Assert
        Assertions.assertTrue(created.getId() >= 0);
        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT uploader_user_id, file_name, content_type, byte_array, created_at "
                        + "FROM stored_files WHERE id = ?",
                created.getId());
        Assertions.assertEquals(UPLOADER_ID, ((Number) row.get("UPLOADER_USER_ID")).longValue());
        Assertions.assertEquals("doc.pdf", row.get("FILE_NAME"));
        Assertions.assertEquals("application/pdf", row.get("CONTENT_TYPE"));
        Assertions.assertArrayEquals(data, (byte[]) row.get("BYTE_ARRAY"));
        Assertions.assertNotNull(row.get("CREATED_AT"));
    }

    @Test
    void testCreateAlsoReturnsTheSameMetadataExposedInTheRow() {
        // 1.Arrange
        insertUser(UPLOADER_ID, "user@example.com", "Ada", "Lovelace");
        final byte[] data = "hello".getBytes();

        // 2.Exercise
        final StoredFile created = dao.create(UPLOADER_ID, "hi.txt", "text/plain", data);

        // 3.Assert
        Assertions.assertEquals(UPLOADER_ID, created.getUploaderUserId());
        Assertions.assertEquals("hi.txt", created.getFileName());
        Assertions.assertEquals("text/plain", created.getContentType());
        Assertions.assertArrayEquals(data, created.getData());
        Assertions.assertNotNull(created.getCreatedAt());
    }

    @Test
    void testFindByIdReturnsPersistedRow() {
        // 1.Arrange
        insertUser(UPLOADER_ID, "user@example.com", "Ada", "Lovelace");
        final long id = 42L;
        final byte[] data = new byte[]{9, 8, 7};
        jdbcTemplate.update(
                "INSERT INTO stored_files(id, uploader_user_id, file_name, content_type, byte_array, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                id, UPLOADER_ID, "fixture.bin", "application/octet-stream", data,
                Timestamp.from(Instant.parse("2026-05-03T13:00:00Z")));

        // 2.Exercise
        final Optional<StoredFile> found = dao.findById(id);

        // 3.Assert
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(id, found.get().getId());
        Assertions.assertEquals(UPLOADER_ID, found.get().getUploaderUserId());
        Assertions.assertEquals("fixture.bin", found.get().getFileName());
        Assertions.assertEquals("application/octet-stream", found.get().getContentType());
        Assertions.assertArrayEquals(data, found.get().getData());
        Assertions.assertNotNull(found.get().getCreatedAt());
    }

    @Test
    void testFindByIdReturnsEmptyWhenIdAbsent() {
        // 1.Arrange  2.Exercise
        final Optional<StoredFile> found = dao.findById(9_999_999L);

        // 3.Assert
        Assertions.assertTrue(found.isEmpty());
    }
}
