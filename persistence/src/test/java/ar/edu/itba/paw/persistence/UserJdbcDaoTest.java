package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

public class UserJdbcDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private UserJdbcDao userDao;

    @Test
    public void testCreateUserNormalizesEmailAndFindByEmailIsCaseInsensitive() {


        // Exercise
        final User created = userDao.createUser("  TEST@MAIL.COM ", "Ada", "Lovelace");
        final Optional<User> found = userDao.findByEmail("test@mail.com");

        // Assert
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(created.getId(), found.get().getId());
        Assertions.assertEquals("test@mail.com", found.get().getEmail());
    }

    @Test
    public void testGetUserByIdWhenNotFound() {


        // Exercise & Assert
        Assertions.assertTrue(userDao.getUserById(55L).isEmpty());
    }

    @Test
    public void testUpdateUserNamePersistsNewValues() {
        // Arrange
        final User created = userDao.createUser("u@mail.com", "Old", "Name");

        // Exercise
        userDao.updateUserName(created.getId(), "New", "Surname");
        final Optional<User> updated = userDao.getUserById(created.getId());

        // Assert
        Assertions.assertTrue(updated.isPresent());
        Assertions.assertEquals("New", updated.get().getForename());
        Assertions.assertEquals("Surname", updated.get().getSurname());
    }

    @Test
    public void testGetListingOwnerByListingId() {
        // Arrange
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertUser(2L, "other@mail.com", "Other", "Two");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", ar.edu.itba.paw.models.Car.Type.HATCHBACK,
                ar.edu.itba.paw.models.Car.Powertrain.GASOLINE, ar.edu.itba.paw.models.Car.Transmission.MANUAL);
        insertListing(100L, 10L, "My listing", ar.edu.itba.paw.models.Listing.Status.ACTIVE,
                new BigDecimal("12.00"), OffsetDateTime.parse("2026-04-01T10:00:00Z"));

        // Exercise
        final Optional<User> owner = userDao.getListingOwner(100L);

        // Assert
        Assertions.assertTrue(owner.isPresent());
        Assertions.assertEquals(1L, owner.get().getId());
        Assertions.assertEquals("owner@mail.com", owner.get().getEmail());
        Assertions.assertTrue(userDao.getListingOwner(999L).isEmpty());
    }

    @Test
    public void testCreateUserWithDuplicateEmailThrowsConstraintViolation() {
        // Arrange
        userDao.createUser("duplicate@mail.com", "A", "A");

        // Exercise & Assert
        Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> userDao.createUser("duplicate@mail.com", "B", "B"));
    }
}

