package ar.edu.itba.paw.services;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.UserDao;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    public void testGetUserByIdWhenUserExists() {
        // 1. Arrange
        final User user = new User(1L, "test@test.com", "TestName", "TestSurname");
        Mockito.when(userDao.getUserById(1L)).thenReturn(Optional.of(user));

        // 2. Execute
        final Optional<User> result = userService.getUserById(1L);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(user, result.get());
        Assertions.assertEquals(1L, result.get().getId());
        Assertions.assertEquals("test@test.com", result.get().getEmail());
        Assertions.assertEquals("TestName", result.get().getForename());
        Assertions.assertEquals("TestSurname", result.get().getSurname());
    }

    @Test
    public void testGetUserByIdWhenUserDoesNotExist() {
        // 1. Arrange
        Mockito.when(userDao.getUserById(Mockito.anyLong())).thenReturn(Optional.empty());

        // 2. Execute
        final Optional<User> result = userService.getUserById(1L);

        // 3. Assert
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testCreateUserWhenUserDoesNotExist() {
        // 1. Arrange
        final User user = new User(1L, "test@test.com", "TestName", "TestSurname");
        Mockito.when(userDao.findByEmail("test@test.com")).thenReturn(Optional.empty());
        Mockito.when(userDao.createUser(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(user);

        // 2. Execute
        final User result = userService.createUser("  Test@Test.COM ", "TestName", "TestSurname");

        // 3. Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1L, result.getId());
        Assertions.assertEquals("test@test.com", result.getEmail());
        Assertions.assertEquals("TestName", result.getForename());
        Assertions.assertEquals("TestSurname", result.getSurname());
    }

    @Test
    public void testCreateUserWhenUserAlreadyExists() {
        // 1. Arrange
        final User existing = new User(1L, "test@test.com", "TestName", "TestSurname");
        Mockito.when(userDao.findByEmail("test@test.com")).thenReturn(Optional.of(existing));

        // 2. Execute
        final EmailAlreadyExistsException thrown = Assertions.assertThrows(EmailAlreadyExistsException.class,
                () -> userService.createUser("  TEST@Test.com ", "TestNameDifferent", "TestSurnameDifferent"));

        // 3. Assert
        Assertions.assertEquals("A user with this email already exists", thrown.getMessage());
    }

    @Test
    public void testFindOrCreatePublisherWhenUserDoesNotExist() {
        final User created = new User(2L, "test@test.com", "Fore", "Sur");
        Mockito.when(userDao.findByEmail("test@test.com")).thenReturn(Optional.empty());
        Mockito.when(userDao.createUser(Mockito.eq("test@test.com"), Mockito.eq("Fore"), Mockito.eq("Sur")))
                .thenReturn(created);

        final User result = userService.findOrCreatePublisher("  Test@Test.COM ", "Fore", "Sur");

        Assertions.assertEquals(2L, result.getId());
        Assertions.assertEquals("test@test.com", result.getEmail());
        Assertions.assertEquals("Fore", result.getForename());
        Assertions.assertEquals("Sur", result.getSurname());
    }

    @Test
    public void testFindOrCreatePublisherWhenUserAlreadyExists() {
        // 1. Arrange
        final User existing = new User(1L, "test@test.com", "OldFore", "OldSur");
        Mockito.when(userDao.findByEmail("test@test.com")).thenReturn(Optional.of(existing));

        // 2. Execute
        final User result = userService.findOrCreatePublisher("  Test@Test.COM ", "NewFore", "NewSur");

        // 3. Assert
        Assertions.assertEquals(1L, result.getId());
        Assertions.assertEquals("test@test.com", result.getEmail());
        Assertions.assertEquals("NewFore", result.getForename());
        Assertions.assertEquals("NewSur", result.getSurname());
    }
}
