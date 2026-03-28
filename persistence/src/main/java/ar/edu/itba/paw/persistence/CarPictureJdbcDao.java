package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.CarPicture;
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
public class CarPictureJdbcDao implements CarPictureDao{
    private final static RowMapper<CarPicture> CAR_PICTURE_ROW_MAPPER = (rs, rowNum) -> new CarPicture(
            rs.getLong("id"),
            rs.getLong("car_id"),
            rs.getLong("image_id"),
            rs.getInt("order")
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
    public CarPicture createCarPicture(final long carId, final long imageId, final int order) {
        final Map<String, Object> values = new HashMap<>();
        values.put("car_id", carId);
        values.put("image_id", imageId);
        values.put("order", order);
        final Number id = jdbcInsert.executeAndReturnKey(values);

        return new CarPicture(id.longValue(), carId, imageId, order);
    }

    @Override
    public Optional<CarPicture> getCarPictureById(final long id) {
        return jdbcTemplate.query("SELECT * FROM car_pictures WHERE id = ?", CAR_PICTURE_ROW_MAPPER, id).stream().findAny();
    }
}
