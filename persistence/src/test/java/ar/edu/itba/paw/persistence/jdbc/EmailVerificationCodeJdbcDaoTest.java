package ar.edu.itba.paw.persistence.jdbc;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.persistence.DaoIntegrationTestSupport;

class EmailVerificationCodeJdbcDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private EmailVerificationCodeJdbcDao dao;

    private static final long USER_ID = 1L;

    private static Instant readInstant(final Object jdbcValue) {
        if (jdbcValue instanceof Timestamp ts) {
            return ts.toInstant();
        }
        if (jdbcValue instanceof OffsetDateTime odt) {
            return odt.toInstant();
        }
        throw new IllegalArgumentException("Unexpected JDBC type for instant: "
                + (jdbcValue == null ? "null" : jdbcValue.getClass().getName()));
    }

    @Test
    void testInsertPersistsRowWithExpectedColumns() {
        // 1.Arrange
        insertUser(USER_ID, "user@example.com", "Ada", "Lovelace");
        final Instant now = Instant.parse("2026-05-03T13:00:00Z");
        final Instant expires = Instant.parse("2026-05-03T13:05:00Z");

        // 2.Exercise
        dao.insert(USER_ID, "123456", expires, now);

        // 3.Assert
        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT user_id, code, expires_at, created_at FROM email_verification_codes WHERE user_id = ?",
                USER_ID);
        Assertions.assertEquals(USER_ID, ((Number) row.get("USER_ID")).longValue());
        Assertions.assertEquals("123456", row.get("CODE"));
        Assertions.assertEquals(expires, readInstant(row.get("EXPIRES_AT")));
        Assertions.assertEquals(now, readInstant(row.get("CREATED_AT")));
    }

    @Test
    void testDeleteForUserRemovesAllRowsForThatUser() {
        // 1.Arrange
        insertUser(USER_ID, "user@example.com", "Ada", "Lovelace");
        insertUser(2L, "other@example.com", "Other", "User");
        final Instant now = Instant.parse("2026-05-03T13:00:00Z");
        final Instant expires = now.plusSeconds(60);
        jdbcTemplate.update(
                "INSERT INTO email_verification_codes (user_id, code, expires_at, created_at) VALUES (?, ?, ?, ?)",
                USER_ID, "111111", Timestamp.from(expires), Timestamp.from(now));
        jdbcTemplate.update(
                "INSERT INTO email_verification_codes (user_id, code, expires_at, created_at) VALUES (?, ?, ?, ?)",
                USER_ID, "222222", Timestamp.from(expires), Timestamp.from(now));
        jdbcTemplate.update(
                "INSERT INTO email_verification_codes (user_id, code, expires_at, created_at) VALUES (?, ?, ?, ?)",
                2L, "333333", Timestamp.from(expires), Timestamp.from(now));

        // 2.Exercise
        dao.deleteForUser(USER_ID);

        // 3.Assert
        final Long forUser = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM email_verification_codes WHERE user_id = ?", Long.class, USER_ID);
        final Long otherUser = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM email_verification_codes WHERE user_id = ?", Long.class, 2L);
        Assertions.assertEquals(0L, forUser);
        Assertions.assertEquals(1L, otherUser, "only the targeted user's rows must be removed");
    }

    @Test
    void testDeleteIfValidRemovesRowAndReturnsTrueWhenCodeMatchesAndStillActive() {
        // 1.Arrange
        insertUser(USER_ID, "user@example.com", "Ada", "Lovelace");
        final Instant now = Instant.parse("2026-05-03T13:00:00Z");
        final Instant expires = now.plusSeconds(60);
        jdbcTemplate.update(
                "INSERT INTO email_verification_codes (user_id, code, expires_at, created_at) VALUES (?, ?, ?, ?)",
                USER_ID, "123456", Timestamp.from(expires), Timestamp.from(now));

        // 2.Exercise
        final boolean ok = dao.deleteIfValid(USER_ID, "  123456  ", now);

        // 3.Assert
        Assertions.assertTrue(ok);
        final Long remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM email_verification_codes WHERE user_id = ?", Long.class, USER_ID);
        Assertions.assertEquals(0L, remaining);
    }

    @Test
    void testDeleteIfValidReturnsFalseAndKeepsRowWhenCodeIsWrong() {
        // 1.Arrange
        insertUser(USER_ID, "user@example.com", "Ada", "Lovelace");
        final Instant now = Instant.parse("2026-05-03T13:00:00Z");
        jdbcTemplate.update(
                "INSERT INTO email_verification_codes (user_id, code, expires_at, created_at) VALUES (?, ?, ?, ?)",
                USER_ID, "123456", Timestamp.from(now.plusSeconds(60)), Timestamp.from(now));

        // 2.Exercise
        final boolean ok = dao.deleteIfValid(USER_ID, "999999", now);

        // 3.Assert
        Assertions.assertFalse(ok);
        final Long remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM email_verification_codes WHERE user_id = ?", Long.class, USER_ID);
        Assertions.assertEquals(1L, remaining);
    }

    @Test
    void testDeleteIfValidReturnsFalseAndKeepsRowWhenCodeAlreadyExpired() {
        // 1.Arrange: row's expires_at <= now → invalid.
        insertUser(USER_ID, "user@example.com", "Ada", "Lovelace");
        final Instant now = Instant.parse("2026-05-03T13:00:00Z");
        jdbcTemplate.update(
                "INSERT INTO email_verification_codes (user_id, code, expires_at, created_at) VALUES (?, ?, ?, ?)",
                USER_ID, "123456", Timestamp.from(now.minusSeconds(1)), Timestamp.from(now.minusSeconds(60)));

        // 2.Exercise
        final boolean ok = dao.deleteIfValid(USER_ID, "123456", now);

        // 3.Assert
        Assertions.assertFalse(ok);
        final Long remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM email_verification_codes WHERE user_id = ?", Long.class, USER_ID);
        Assertions.assertEquals(1L, remaining);
    }

    @Test
    void testHasActiveCodeReturnsTrueWhenAtLeastOneRowIsStillFresh() {
        // 1.Arrange
        insertUser(USER_ID, "user@example.com", "Ada", "Lovelace");
        final Instant now = Instant.parse("2026-05-03T13:00:00Z");
        jdbcTemplate.update(
                "INSERT INTO email_verification_codes (user_id, code, expires_at, created_at) VALUES (?, ?, ?, ?)",
                USER_ID, "111111", Timestamp.from(now.minusSeconds(1)), Timestamp.from(now.minusSeconds(60)));
        jdbcTemplate.update(
                "INSERT INTO email_verification_codes (user_id, code, expires_at, created_at) VALUES (?, ?, ?, ?)",
                USER_ID, "222222", Timestamp.from(now.plusSeconds(60)), Timestamp.from(now));

        // 2.Exercise
        final boolean active = dao.hasActiveCode(USER_ID, now);

        // 3.Assert
        Assertions.assertTrue(active);
    }

    @Test
    void testHasActiveCodeReturnsFalseWhenAllCodesExpired() {
        // 1.Arrange
        insertUser(USER_ID, "user@example.com", "Ada", "Lovelace");
        final Instant now = Instant.parse("2026-05-03T13:00:00Z");
        jdbcTemplate.update(
                "INSERT INTO email_verification_codes (user_id, code, expires_at, created_at) VALUES (?, ?, ?, ?)",
                USER_ID, "111111", Timestamp.from(now.minusSeconds(1)), Timestamp.from(now.minusSeconds(60)));

        // 2.Exercise
        final boolean active = dao.hasActiveCode(USER_ID, now);

        // 3.Assert
        Assertions.assertFalse(active);
    }

    @Test
    void testHasActiveCodeReturnsFalseWhenNoRowsForUser() {
        // 1.Arrange
        insertUser(USER_ID, "user@example.com", "Ada", "Lovelace");

        // 2.Exercise
        final boolean active = dao.hasActiveCode(USER_ID, Instant.parse("2026-05-03T13:00:00Z"));

        // 3.Assert
        Assertions.assertFalse(active);
    }
}
