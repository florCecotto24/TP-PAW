package ar.edu.itba.paw.persistence.jdbc;

import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.persistence.DaoIntegrationTestSupport;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.User;

public class UserJdbcDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private UserDao userDao;

    @Test
    public void testCreateUserNormalizesEmailPersistsRow() {
        final LocalDate todayWall = LocalDate.now(AvailabilityPeriod.WALL_ZONE);

        // Exercise
        final User created = userDao.createUser("  TEST@MAIL.COM ", "Ada", "Lovelace");

        Assertions.assertEquals(Optional.of(false), created.getEmailValidated());
        Assertions.assertEquals(Optional.of(todayWall), created.getMemberSince());

        // Assert 
        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT email, forename, surname, email_validated, member_since FROM users WHERE id = ?",
                created.getId());
        Assertions.assertEquals("test@mail.com", row.get("EMAIL"));
        Assertions.assertEquals("Ada", row.get("FORENAME"));
        Assertions.assertEquals("Lovelace", row.get("SURNAME"));
        Assertions.assertEquals(Boolean.FALSE, row.get("EMAIL_VALIDATED"));
        Assertions.assertEquals(todayWall, ((Date) row.get("MEMBER_SINCE")).toLocalDate());
    }

    @Test
    public void testFindByEmailIsCaseInsensitive() {
        insertUser(1L, "test@mail.com", "A", "B");

        final Optional<User> found = userDao.findByEmail("TEST@mail.com");

        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(1L, found.get().getId());
        Assertions.assertEquals("test@mail.com", found.get().getEmail());
    }

    @Test
    public void testGetUserByIdWhenNotFound() {


        // Exercise & Assert
        Assertions.assertTrue(userDao.getUserById(55L).isEmpty());
    }

    @Test
    public void testUpdateUserNamePersistsNewValues() {
        insertUser(200L, "u@mail.com", "Old", "Name");

        // Exercise
        userDao.updateUserName(200L, "New", "Surname");

        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT forename, surname FROM users WHERE id = ?", 200L);
        Assertions.assertEquals("New", row.get("FORENAME"));
        Assertions.assertEquals("Surname", row.get("SURNAME"));
    }

    @Test
    public void testUpdateOptionalPhoneNumberSetsAndClearsWithoutTouchingBirthDate() {
        insertUser(201L, "test@mail.com", "Test", "User");
        final LocalDate birth = LocalDate.of(1990, 5, 15);
        jdbcTemplate.update("UPDATE users SET birth_date = ? WHERE id = ?", Date.valueOf(birth), 201L);

        userDao.updatePhoneNumber(201L, "+5491112345678");
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT phone_number, birth_date FROM users WHERE id = ?", 201L);
        Assertions.assertEquals("+5491112345678", row.get("PHONE_NUMBER"));
        Assertions.assertEquals(birth, ((Date) row.get("BIRTH_DATE")).toLocalDate());

        userDao.updatePhoneNumber(201L, null);
        row = jdbcTemplate.queryForMap("SELECT phone_number, birth_date FROM users WHERE id = ?", 201L);
        Assertions.assertNull(row.get("PHONE_NUMBER"));
        Assertions.assertEquals(birth, ((Date) row.get("BIRTH_DATE")).toLocalDate());
    }

    @Test
    public void testUpdateOptionalBirthDateSetsAndClearsWithoutTouchingPhone() {
        insertUser(202L, "birth@mail.com", "Test", "User");
        jdbcTemplate.update("UPDATE users SET phone_number = ? WHERE id = ?", "+5411", 202L);
        final LocalDate birth = LocalDate.of(1991, 6, 20);

        userDao.updateBirthDate(202L, birth);
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT birth_date, phone_number FROM users WHERE id = ?", 202L);
        Assertions.assertEquals(birth, ((Date) row.get("BIRTH_DATE")).toLocalDate());
        Assertions.assertEquals("+5411", row.get("PHONE_NUMBER"));

        userDao.updateBirthDate(202L, null);
        row = jdbcTemplate.queryForMap("SELECT birth_date, phone_number FROM users WHERE id = ?", 202L);
        Assertions.assertNull(row.get("BIRTH_DATE"));
        Assertions.assertEquals("+5411", row.get("PHONE_NUMBER"));
    }

    @Test
    public void testUpdateOptionalAboutSetsAndClears() {
        insertUser(203L, "about@mail.com", "Test", "User");

        userDao.updateAbout(203L, "Host and rider.");
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT about FROM users WHERE id = ?", 203L);
        Assertions.assertEquals("Host and rider.", row.get("ABOUT"));

        userDao.updateAbout(203L, null);
        row = jdbcTemplate.queryForMap("SELECT about FROM users WHERE id = ?", 203L);
        Assertions.assertNull(row.get("ABOUT"));
    }

    @Test
    public void testGetListingOwnerByListingId() {
        // Arrange
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertUser(2L, "other@mail.com", "Other", "Two");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", ar.edu.itba.paw.models.domain.Car.Type.HATCHBACK,
                ar.edu.itba.paw.models.domain.Car.Powertrain.GASOLINE, ar.edu.itba.paw.models.domain.Car.Transmission.MANUAL);
        insertListing(100L, 10L, "My listing", ar.edu.itba.paw.models.domain.Listing.Status.ACTIVE,
                new BigDecimal("12.00"), OffsetDateTime.parse("2026-04-01T10:00:00Z"));

        // Exercise
        final Optional<User> owner = userDao.getListingOwner(100L);

        // Assert
        Assertions.assertTrue(owner.isPresent());
        Assertions.assertEquals(1L, owner.get().getId());
        Assertions.assertEquals("owner@mail.com", owner.get().getEmail());
    }

    @Test
    public void testGetListingOwnerWhenListingMissingReturnsEmpty() {
        Assertions.assertTrue(userDao.getListingOwner(999L).isEmpty());
    }

    @Test
    public void testFindByEmailForAuthenticationLoadsPasswordHash() {
        final String bcrypt = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";
        insertUser(1L, "auth@mail.com", "Auth", "User", bcrypt);

        final Optional<User> found = userDao.findByEmailForAuthentication("AUTH@mail.com");

        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(bcrypt, found.get().getPasswordHash().orElseThrow());
    }

    @Test
    public void testCreateUserWithDuplicateEmailThrowsConstraintViolation() {
        insertUser(1L, "duplicate@mail.com", "A", "A");

        Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> userDao.createUser("duplicate@mail.com", "B", "B"));
    }
}
