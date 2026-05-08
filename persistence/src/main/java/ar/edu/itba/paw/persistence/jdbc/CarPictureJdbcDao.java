package ar.edu.itba.paw.persistence.jdbc;

import ar.edu.itba.paw.persistence.CarPictureDao;
import ar.edu.itba.paw.persistence.util.JdbcDateTimeUtils;
import ar.edu.itba.paw.models.domain.CarPicture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
// @Repository removed — replaced by CarPictureHibernateDao

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CarPictureJdbcDao implements CarPictureDao {

    private static final String SELECT_COLUMNS =
            "id, car_id, image_id, display_order, created_at, updated_at";

    private static final RowMapper<CarPicture> CAR_PICTURE_ROW_MAPPER = (rs, rowNum) -> new CarPicture(
            rs.getLong("id"),
            rs.getLong("car_id"),
            rs.getLong("image_id"),
            rs.getInt("display_order"),
            JdbcDateTimeUtils.toOffsetDateTime(rs.getTimestamp("created_at")),
            JdbcDateTimeUtils.toOffsetDateTime(rs.getTimestamp("updated_at"))
    );

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Autowired
    public CarPictureJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("car_pictures")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public CarPicture createCarPicture(final long carId, final long imageId, final int displayOrder) {
        final Timestamp now = JdbcDateTimeUtils.nowTimestamp();
        final Map<String, Object> values = new HashMap<>();
        values.put("car_id", carId);
        values.put("image_id", imageId);
        values.put("display_order", displayOrder);
        values.put("created_at", now);
        values.put("updated_at", now);
        final Number id = jdbcInsert.executeAndReturnKey(values);

        return new CarPicture(
                id.longValue(),
                carId,
                imageId,
                displayOrder,
                JdbcDateTimeUtils.toOffsetDateTime(now),
                JdbcDateTimeUtils.toOffsetDateTime(now));
    }

    @Override
    public Optional<CarPicture> getCarPictureById(final long id) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM car_pictures WHERE id = ?",
                CAR_PICTURE_ROW_MAPPER,
                id).stream()
                .findAny();
    }

    @Override
    public List<CarPicture> getCarPicturesByCarId(final long carId) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM car_pictures WHERE car_id = ? ORDER BY display_order",
                CAR_PICTURE_ROW_MAPPER,
                carId);
    }
}
