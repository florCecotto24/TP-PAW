package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class ImageJdbcDao implements ImageDao{
    private final static RowMapper<Image> IMAGE_ROW_MAPPER = (rs, rowNum) -> new Image(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("content_type"),
            rs.getBytes("data")
    );

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Autowired
    public ImageJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("images")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public Image createImage(final String name, final String contentType, final byte[] data) {
        final Map<String, Object> values = new HashMap<>();
        values.put("name", name);
        values.put("content_type", contentType);
        values.put("data", data);
        final Number id = jdbcInsert.executeAndReturnKey(values);

        return new Image(id.longValue(), name, contentType, data);
    }

    @Override
    public Optional<Image> getImageById(final long id) {
        return jdbcTemplate.query("SELECT * FROM images WHERE id = ?", IMAGE_ROW_MAPPER, id).stream().findAny();
    }
}
