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

    private static final String SELECT_COLUMNS = "id, email, forename, surname";

    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("forename"),
            rs.getString("surname")
    );

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Autowired
    public UserJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert = new SimpleJdbcInsert(dataSource).withTableName("users").usingGeneratedKeyColumns("id");
    }

    @Override
    public User createUser(final String email, final String forename, final String surname) {
        final Map<String, Object> values = new HashMap<>();
        values.put("email", email);
        values.put("forename", forename);
        values.put("surname", surname);
        final Number userId = jdbcInsert.executeAndReturnKey(values);
        return new User(userId.longValue(), email, forename, surname);
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
    public void updateUserName(final long userId, final String forename, final String surname) {
         jdbcTemplate.update("UPDATE users SET forename = ?, surname = ? WHERE id = ?", forename, surname, userId);
    }
}
