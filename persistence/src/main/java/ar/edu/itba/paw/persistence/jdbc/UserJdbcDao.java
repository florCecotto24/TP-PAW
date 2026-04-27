package ar.edu.itba.paw.persistence.jdbc;

import ar.edu.itba.paw.persistence.UserDao;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import ar.edu.itba.paw.models.util.EmailNormalizer;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.security.UserRole;

@Repository
public class UserJdbcDao implements UserDao {

    private static final String SELECT_COLUMNS =
            "id, email, forename, surname, email_validated, phone_number, birth_date, about, profile_picture_id, latest_locale";

    private static Long readNullableLongId(final ResultSet rs, final String column) throws SQLException {
        final Object v = rs.getObject(column);
        if (v == null) {
            return null;
        }
        if (v instanceof Long) {
            return (Long) v;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return Long.parseLong(v.toString());
    }

    private static User mapUser(final ResultSet rs, final boolean includePasswordHash) throws SQLException {
        final java.sql.Date birth = rs.getDate("birth_date");
        final String passwordHash = includePasswordHash ? rs.getString("password_hash") : null;
        return User.builder()
                .id(rs.getLong("id"))
                .email(rs.getString("email"))
                .forename(rs.getString("forename"))
                .surname(rs.getString("surname"))
                .passwordHash(passwordHash)
                .emailValidated(rs.getObject("email_validated", Boolean.class))
                .phoneNumber(rs.getString("phone_number"))
                .birthDate(birth != null ? birth.toLocalDate() : null)
                .about(rs.getString("about"))
                .profilePictureId(readNullableLongId(rs, "profile_picture_id"))
                .latestLocaleTag(rs.getString("latest_locale"))
                .build();
    }

    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> mapUser(rs, false);

    private static final RowMapper<User> USER_WITH_PASSWORD_ROW_MAPPER = (rs, rowNum) -> mapUser(rs, true);

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Autowired
    public UserJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("users")
                .usingColumns("email", "forename", "surname", "password_hash", "email_validated")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public User createUser(final String email, final String forename, final String surname, final String passwordHash) {
        final String normalizedEmail = EmailNormalizer.normalize(email);
        final Map<String, Object> values = new HashMap<>();
        values.put("email", normalizedEmail);
        values.put("forename", forename);
        values.put("surname", surname);
        values.put("password_hash", passwordHash);
        values.put("email_validated", Boolean.FALSE);
        final Number userId = jdbcInsert.executeAndReturnKey(values);
        insertUserRole(userId.longValue(), UserRole.USER);
        return User.builder()
                .id(userId.longValue())
                .email(normalizedEmail)
                .forename(forename)
                .surname(surname)
                .passwordHash(passwordHash)
                .emailValidated(Boolean.FALSE)
                .build();
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
        final String normalizedEmail = EmailNormalizer.normalize(email);
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM users WHERE LOWER(TRIM(email)) = LOWER(TRIM(?))",
                USER_ROW_MAPPER,
                normalizedEmail).stream().findFirst();
    }

    @Override
    public Optional<User> findByEmailForAuthentication(final String email) {
        final String normalizedEmail = EmailNormalizer.normalize(email);
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + ", password_hash FROM users WHERE LOWER(TRIM(email)) = LOWER(TRIM(?))",
                USER_WITH_PASSWORD_ROW_MAPPER,
                normalizedEmail).stream().findFirst();
    }

    @Override
    public void updateUserName(final long userId, final String forename, final String surname) {
        jdbcTemplate.update("UPDATE users SET forename = ?, surname = ? WHERE id = ?", forename, surname, userId);
    }

    @Override
    public void updatePhoneNumber(final long userId, final String phoneNumber) {
        jdbcTemplate.update("UPDATE users SET phone_number = ? WHERE id = ?", phoneNumber, userId);
    }

    @Override
    public void updateBirthDate(final long userId, final LocalDate birthDate) {
        final Date birthSql = birthDate == null ? null : Date.valueOf(birthDate);
        jdbcTemplate.update("UPDATE users SET birth_date = ? WHERE id = ?", birthSql, userId);
    }

    @Override
    public void updateAbout(final long userId, final String about) {
        jdbcTemplate.update("UPDATE users SET about = ? WHERE id = ?", about, userId);
    }

    @Override
    public void updateProfilePictureId(final long userId, final Long profilePictureImageId) {
        jdbcTemplate.update("UPDATE users SET profile_picture_id = ? WHERE id = ?", profilePictureImageId, userId);
    }

    @Override
    public void updateEmailValidated(final long userId, final boolean validated) {
        jdbcTemplate.update("UPDATE users SET email_validated = ? WHERE id = ?", validated, userId);
    }

    @Override
    public void updatePasswordHash(final long userId, final String passwordHash) {
        jdbcTemplate.update("UPDATE users SET password_hash = ? WHERE id = ?", passwordHash, userId);
    }

    @Override
    public Optional<User> getListingOwner(final long listingId) {
        return jdbcTemplate.query(
                "SELECT u.id, u.email, u.forename, u.surname, u.email_validated, u.phone_number, u.birth_date, "
                        + "u.about, u.profile_picture_id, u.latest_locale FROM users u "
                        + "JOIN cars c ON c.owner_id = u.id "
                        + "JOIN listings l ON l.car_id = c.id "
                        + "WHERE l.id = ?",
                USER_ROW_MAPPER,
                listingId).stream().findAny();
    }

    @Override
    public void updateLatestLocale(final long userId, final String localeTag) {
        jdbcTemplate.update("UPDATE users SET latest_locale = ? WHERE id = ?", localeTag, userId);
    }

    @Override
    public List<String> findRoleNamesForUser(final long userId) {
        return jdbcTemplate.query(
                "SELECT role FROM user_roles WHERE user_id = ? ORDER BY role",
                (rs, rn) -> rs.getString("role"),
                userId);
    }

    @Override
    public void insertUserRole(final long userId, final UserRole role) {
        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role) VALUES (?, ?)",
                userId,
                role.persistenceName());
    }
}
