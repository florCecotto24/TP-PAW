package ar.edu.itba.paw.persistence.user;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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

        // 3.Assert
        final Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM email_verification_codes WHERE user_id = ? AND code = ?",
                Long.class,
                userId,
                "123456");
        Assertions.assertEquals(1L, count);
        Assertions.assertTrue(dao.hasActiveCode(userId, now.plus(30, ChronoUnit.MINUTES)));
    }

    @Test
    void testDeleteIfValidRemovesMatchingUnexpiredCode() {
        // 1.Arrange
        final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        final Instant expires = now.plus(1, ChronoUnit.HOURS);
        dao.insert(userId, "654321", expires, now);
        em.flush();

        // 2.Act
        final boolean deleted = dao.deleteIfValid(userId, "654321", now);
        em.flush();

        // 3.Assert
        Assertions.assertTrue(deleted);
        Assertions.assertFalse(dao.hasActiveCode(userId, now));
    }

    @Test
    void testDeleteForUserRemovesAllCodesForUser() {
        // 1.Arrange
        final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        dao.insert(userId, "111111", now.plus(1, ChronoUnit.HOURS), now);
        em.flush();

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
}
