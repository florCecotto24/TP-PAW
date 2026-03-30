package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
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
public class UserJdbcDao implements UserDao {

    private static final String SELECT_COLUMNS = "id, email, name";

    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> new User(
            rs.getLong(1),
            rs.getString(2),
            rs.getString(3)
    );

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Autowired
    public UserJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert = new SimpleJdbcInsert(dataSource).withTableName("users").usingGeneratedKeyColumns("id");
    }

    @Override
    public User createUser(final String email, final String name) {
        final Map<String, Object> values = new HashMap<>();
        values.put("email", email);
        values.put("name", name);
        final Number userId = jdbcInsert.executeAndReturnKey(values);
        return new User(userId.longValue(), email, name);
    }

    @Override
    public Optional<User> getUserById(final long id) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM users WHERE id = ?",
                USER_ROW_MAPPER,
                id).stream().findAny();
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM users WHERE LOWER(TRIM(email)) = LOWER(TRIM(?))",
                USER_ROW_MAPPER,
                email).stream().findFirst();
    }

    @Override
    public void updateUserName(final long userId, final String name) {
        jdbcTemplate.update("UPDATE users SET name = ? WHERE id = ?", name, userId);
    }
}
