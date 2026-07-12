package ar.edu.itba.paw.persistence.user;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.security.UserRole;
import ar.edu.itba.paw.persistence.support.DaoIntegrationTestSupport;

class UserJpaDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private UserDao dao;

    @PersistenceContext
    private EntityManager em;

    @Test
    void testCreateUserPersistsNormalizedEmailAndRole() {
        // 1.Arrange — empty users table in rolled-back test transaction.

        // 2.Act
        final User created = dao.createUser(
                "  Test@Example.COM ",
                "Ada",
                "Lovelace",
                "HASH",
                UserRole.USER,
                false,
                null);
        em.flush();

        // 3.Assert — verify via JDBC, not another DAO read.
        final String email = jdbcTemplate.queryForObject(
                "SELECT email FROM users WHERE id = ?", String.class, created.getId());
        final String role = jdbcTemplate.queryForObject(
                "SELECT user_role FROM users WHERE id = ?", String.class, created.getId());
        Assertions.assertEquals("test@example.com", email);
        Assertions.assertEquals("USER", role);
    }

    @Test
    void testUpdateUserNameMutatesForenameAndSurname() {
        // 1.Arrange
        final User created = dao.createUser(
                "rename@test.com", "Old", "Name", "HASH", UserRole.USER, true, null);
        em.flush();

        // 2.Act
        dao.updateUserName(created.getId(), "New", "Surname");
        em.flush();

        // 3.Assert
        final String forename = jdbcTemplate.queryForObject(
                "SELECT forename FROM users WHERE id = ?", String.class, created.getId());
        final String surname = jdbcTemplate.queryForObject(
                "SELECT surname FROM users WHERE id = ?", String.class, created.getId());
        Assertions.assertEquals("New", forename);
        Assertions.assertEquals("Surname", surname);
    }
}
