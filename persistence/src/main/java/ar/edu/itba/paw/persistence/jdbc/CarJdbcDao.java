package ar.edu.itba.paw.persistence.jdbc;

import ar.edu.itba.paw.persistence.CarDao;
import ar.edu.itba.paw.models.Car;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CarJdbcDao implements CarDao{

    private final static RowMapper<Car> CAR_ROW_MAPPER = (rs, rowNum) -> new Car(
            rs.getLong("id"),
            rs.getLong("owner_id"),
            rs.getString("plate"),
            rs.getString("brand"),
            rs.getString("model"),
            Car.Type.valueOf(rs.getString("type")),
            Car.Powertrain.valueOf(rs.getString("powertrain")),
            Car.Transmission.valueOf(rs.getString("transmission"))
    );

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Autowired
    public CarJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("cars")
                .usingGeneratedKeyColumns("id");
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

        return new Car(id.longValue(), ownerId, plate, brand, model, type, powertrain, transmission);
    }

    @Override
    public Optional<Car> getCarById(final long id) {
        return jdbcTemplate.query("SELECT * FROM cars WHERE id = ?", CAR_ROW_MAPPER, id).stream().findAny();
        //ponemos findAny porque el id es unico, entonces o devuelve un resultado o no devuelve nada
    }


    //Esto hay que terminar de arreglarlo, no sé cómo vamos a elegir los autos destacados ni los más buscados, por ahora solo devuelve los primeros 8 coches de la base de datos
    @Override
    public List<Car> getCheapestCars() {
        return jdbcTemplate.query(
                "SELECT cars.* FROM cars " +
                        "JOIN listings ON listings.car_id = cars.id " +
                        "WHERE listings.status = 'active' " +
                        "ORDER BY listings.day_price ASC " +
                        "LIMIT 8",
                CAR_ROW_MAPPER
        );
    }

    @Override
    public List<Car> getMostRecentCars() {
        return jdbcTemplate.query(
                "SELECT cars.* FROM cars " +
                        "JOIN listings ON listings.car_id = cars.id " +
                        "WHERE listings.status = 'active' " +
                        "ORDER BY listings.created_at DESC " +
                        "LIMIT 8",
                CAR_ROW_MAPPER
        );
    }

}
