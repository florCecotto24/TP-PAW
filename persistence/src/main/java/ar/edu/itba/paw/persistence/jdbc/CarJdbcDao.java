package ar.edu.itba.paw.persistence.jdbc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.persistence.CarDao;

@Repository
public final class CarJdbcDao implements CarDao {

    private final static RowMapper<Car> CAR_ROW_MAPPER = (rs, rowNum) -> Car.builder()
            .id(rs.getLong("id"))
            .ownerId(rs.getLong("owner_id"))
            .plate(rs.getString("plate"))
            .brand(rs.getString("brand"))
            .model(rs.getString("model"))
            .type(Car.Type.valueOf(rs.getString("type")))
            .powertrain(Car.Powertrain.valueOf(rs.getString("powertrain")))
            .transmission(Car.Transmission.valueOf(rs.getString("transmission")))
            .build();

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;
    private final int carCatalogLimit;

    @Autowired
    public CarJdbcDao(
            final DataSource dataSource,
            @Value("${app.listing.car-catalog-limit:8}") final int carCatalogLimit) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("cars")
                .usingGeneratedKeyColumns("id");
        this.carCatalogLimit = Math.max(1, carCatalogLimit);
    }

    @Override
    public Car createCar(final long ownerId, final String plate, final String brand, final String model,
                         final Car.Type type, final Car.Powertrain powertrain, final Car.Transmission transmission) {
        final Map<String, Object> values = new HashMap<>();
        values.put("owner_id", ownerId);
        values.put("plate", plate);
        values.put("brand", brand);
        values.put("model", model);
        values.put("type", type.name());
        values.put("powertrain", powertrain.name());
        values.put("transmission", transmission.name());
        final Number id = jdbcInsert.executeAndReturnKey(values);

        return Car.builder()
                .id(id.longValue())
                .ownerId(ownerId)
                .plate(plate)
                .brand(brand)
                .model(model)
                .type(type)
                .powertrain(powertrain)
                .transmission(transmission)
                .build();
    }

    @Override
    public Optional<Car> getCarById(final long id) {
        // Primary key: at most one row, so findAny is equivalent to findFirst.
        return jdbcTemplate.query("SELECT * FROM cars WHERE id = ?", CAR_ROW_MAPPER, id).stream().findAny();
    }


    @Override
    public List<Car> getCheapestCars() {
        return jdbcTemplate.query(
                "SELECT cars.* FROM cars " +
                        "JOIN listings ON listings.car_id = cars.id " +
                        "WHERE listings.status = 'active' " +
                        "ORDER BY listings.day_price ASC " +
                        "LIMIT ?",
                CAR_ROW_MAPPER,
                carCatalogLimit);
    }

    @Override
    public List<Car> getMostRecentCars() {
        return jdbcTemplate.query(
                "SELECT cars.* FROM cars " +
                        "JOIN listings ON listings.car_id = cars.id " +
                        "WHERE listings.status = 'active' " +
                        "ORDER BY listings.created_at DESC " +
                        "LIMIT ?",
                CAR_ROW_MAPPER,
                carCatalogLimit);
    }

}
