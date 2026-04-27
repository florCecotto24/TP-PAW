package ar.edu.itba.paw.persistence.jdbc;

import ar.edu.itba.paw.persistence.StoredFileDao;
import ar.edu.itba.paw.persistence.util.JdbcDateTimeUtils;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import ar.edu.itba.paw.models.domain.StoredFile;

@Repository
public class StoredFileJdbcDao implements StoredFileDao {

    private static final RowMapper<StoredFile> ROW_MAPPER = (rs, rowNum) -> new StoredFile(
            rs.getLong("id"),
            rs.getLong("uploader_user_id"),
            rs.getString("file_name"),
            rs.getString("content_type"),
            rs.getBytes("byte_array"),
            JdbcDateTimeUtils.readOffsetDateTime(rs, "created_at"));

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Autowired
    public StoredFileJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("stored_files")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public StoredFile create(final long uploaderUserId, final String fileName, final String contentType, final byte[] data) {
        final Timestamp now = JdbcDateTimeUtils.nowTimestamp();
        final Map<String, Object> values = new HashMap<>();
        values.put("uploader_user_id", uploaderUserId);
        values.put("file_name", fileName);
        values.put("content_type", contentType);
        values.put("byte_array", data);
        values.put("created_at", now);
        final Number id = jdbcInsert.executeAndReturnKey(values);
        return new StoredFile(
                id.longValue(),
                uploaderUserId,
                fileName,
                contentType,
                data,
                JdbcDateTimeUtils.toOffsetDateTime(now));
    }

    @Override
    public Optional<StoredFile> findById(final long id) {
        return jdbcTemplate.query(
                "SELECT id, uploader_user_id, file_name, content_type, byte_array, created_at FROM stored_files WHERE id = ?",
                ROW_MAPPER,
                id).stream().findAny();
    }
}
