package ar.edu.itba.paw.persistence.user;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.sql.Timestamp;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.persistence.support.DaoIntegrationTestSupport;

class EmailVerificationCodeJpaDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private EmailVerificationCodeDao dao;

    @PersistenceContext
    private EntityManager em;

    private long userId;

    @BeforeEach
    void seedUser() {
        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "evc@test.com", "Evc", "User");
        userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, "evc@test.com");
    }

    @Test
    void testInsertPersistsActiveCodeRow() {
        // 1.Arrange
        final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        final Instant expires = now.plus(1, ChronoUnit.HOURS);

        // 2.Act
        dao.insert(userId, "123456", expires, now);
        em.flush();

        // 3.Assert — ground truth via JDBC (not another DAO read).
        final Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM email_verification_codes WHERE user_id = ? AND code = ?",
                Long.class,
                userId,
                "123456");
        Assertions.assertEquals(1L, count);
    }

    @Test
    void testHasActiveCodeReturnsTrueForUnexpiredSeededRow() {
        // 1.Arrange — seed via JDBC so Act only exercises the read under test.
        final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        insertCode("123456", now.plus(1, ChronoUnit.HOURS), now);

        // 2.Act / 3.Assert
        Assertions.assertTrue(dao.hasActiveCode(userId, now.plus(30, ChronoUnit.MINUTES)));
    }

    @Test
    void testDeleteIfValidRemovesMatchingUnexpiredCode() {
        // 1.Arrange
        final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        insertCode("654321", now.plus(1, ChronoUnit.HOURS), now);

        // 2.Act
        final boolean deleted = dao.deleteIfValid(userId, "654321", now);
        em.flush();

        // 3.Assert
        Assertions.assertTrue(deleted);
        final Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM email_verification_codes WHERE user_id = ? AND code = ?",
                Long.class,
                userId,
                "654321");
        Assertions.assertEquals(0L, count);
    }

    @Test
    void testDeleteForUserRemovesAllCodesForUser() {
        // 1.Arrange
        final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        insertCode("111111", now.plus(1, ChronoUnit.HOURS), now);

        // 2.Act
        dao.deleteForUser(userId);
        em.flush();

        // 3.Assert
        final Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM email_verification_codes WHERE user_id = ?",
                Long.class,
                userId);
        Assertions.assertEquals(0L, count);
    }

    private void insertCode(final String code, final Instant expiresAt, final Instant createdAt) {
        jdbcTemplate.update(
                "INSERT INTO email_verification_codes (user_id, code, expires_at, created_at) VALUES (?, ?, ?, ?)",
                userId,
                code,
                Timestamp.from(expiresAt),
                Timestamp.from(createdAt));
    }
}
