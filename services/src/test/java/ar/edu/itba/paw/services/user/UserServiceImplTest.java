package ar.edu.itba.paw.services.user;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.admin.AdminPromoterNotAdminException;
import ar.edu.itba.paw.exception.admin.UserAlreadyAdminException;
import ar.edu.itba.paw.exception.user.EmailAlreadyExistsException;
import ar.edu.itba.paw.exception.user.InvalidProfileBirthDateException;
import ar.edu.itba.paw.exception.user.InvalidProfilePhoneException;
import ar.edu.itba.paw.exception.user.InvalidUserFieldLengthException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.security.UserRole;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.policy.UserValidationPolicy;

import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.email.EmailService;
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UserServiceImplTest {

    @Mock
    private UserDao userDao;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private CarService carService;

    @Mock
    private UserProfileMediaService userProfileMediaService;

    private UserServiceImpl userService;

    @BeforeEach
    public void setUp() {
        userService = new UserServiceImpl(
                userDao,
                emailService,
                passwordEncoder,
                UserValidationPolicy.fromValidatedConfiguration(8, 72, 50, 50, 20, 500, "^[0-9+]+$"),
                emailVerificationService,
                carService,
                userProfileMediaService);
    }

    @Test
    public void testGetUserByIdWhenUserExists() {
        // 1. Arrange
        final User user = User.identities(1L, "test@test.com", "TestName", "TestSurname");
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
    public void testRegisterUserWhenUserDoesNotExist() {
        // 1. Arrange
        final User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .forename("TestName")
                .surname("TestSurname")
                .passwordHash("ENC")
                .emailValidated(false)
                .build();
        Mockito.when(userDao.findByEmail("test@test.com")).thenReturn(Optional.empty());
        Mockito.when(passwordEncoder.encode(Mockito.anyString())).thenReturn("ENC");
        Mockito.when(userDao.createUser(Mockito.eq("test@test.com"), Mockito.eq("TestName"), Mockito.eq("TestSurname"), Mockito.eq("ENC")))
                .thenReturn(user);

        // 2. Execute
        final User result = userService.registerUser(
                "  Test@Test.COM ", "TestName", "TestSurname", "password12", "password12");

        // 3. Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1L, result.getId());
        Assertions.assertEquals("test@test.com", result.getEmail());
        Assertions.assertEquals("TestName", result.getForename());
        Assertions.assertEquals("TestSurname", result.getSurname());
    }

    @Test
    public void testRegisterUserWhenUserAlreadyExists() {
        // 1. Arrange
        final User existing = User.identities(1L, "test@test.com", "TestName", "TestSurname");
        Mockito.when(userDao.findByEmail("test@test.com")).thenReturn(Optional.of(existing));

        // 2. Execute and 3. Assert
        final EmailAlreadyExistsException thrown = Assertions.assertThrows(EmailAlreadyExistsException.class,
                () -> userService.registerUser(
                        "  TEST@Test.com ", "TestNameDifferent", "TestSurnameDifferent", "password12", "password12"));
        Assertions.assertEquals(MessageKeys.USER_EMAIL_ALREADY_EXISTS, thrown.getMessageCode());
    }

    @Test
    public void testUpdateDisplayNameWhenUserExistsDoesNotThrow() {
        // 1. Arrange
        final User user = User.identities(1L, "u@mail.com", "A", "B");
        Mockito.when(userDao.getUserById(1L)).thenReturn(Optional.of(user));

        // 2. Execute and 3. Assert
        Assertions.assertDoesNotThrow(() -> userService.updateDisplayName(1L, "  Ada ", " Lovelace "));

    }

    @Test
    public void testUpdateDisplayNameUserMissingThrows() {
        // 1. Arrange
        Mockito.when(userDao.getUserById(99L)).thenReturn(Optional.empty());

        Assertions.assertThrows(UserNotFoundException.class,
                () -> userService.updateDisplayName(99L, "X", "Y"));
    }

    @Test
    public void testUpdatePhoneNumberWhenValidDoesNotThrow() {
        // 1. Arrange
        final User user = User.identities(1L, "u@mail.com", "A", "B");
        Mockito.when(userDao.getUserById(1L)).thenReturn(Optional.of(user));

        // 2. Execute and 3. Assert
        Assertions.assertDoesNotThrow(() -> userService.updatePhoneNumber(1L, "  +5411  "));
    }

    @Test
    public void testUpdateBirthDateDoesNotThrow() {
        // 1. Arrange
        final User user = User.identities(1L, "u@mail.com", "A", "B");
        Mockito.when(userDao.getUserById(1L)).thenReturn(Optional.of(user));
        final LocalDate birth = LocalDate.of(2000, 1, 2);

        // 2. Execute and 3. Assert
        Assertions.assertDoesNotThrow(() -> userService.updateBirthDate(1L, birth));
    }

    @Test
    public void testUpdateBirthDateTodayDoesNotThrow() {
        // 1. Arrange
        final User user = User.identities(1L, "u@mail.com", "A", "B");
        Mockito.when(userDao.getUserById(1L)).thenReturn(Optional.of(user));
        final LocalDate today = LocalDate.now(AppTimezone.WALL_ZONE);

        // 2. Execute and 3. Assert
        Assertions.assertDoesNotThrow(() -> userService.updateBirthDate(1L, today));
    }

    @Test
    public void testUpdateBirthDateFutureThrows() {
        // 1. Arrange
        final User user = User.identities(1L, "u@mail.com", "A", "B");
        Mockito.when(userDao.getUserById(1L)).thenReturn(Optional.of(user));
        final LocalDate future = LocalDate.now(AppTimezone.WALL_ZONE).plusDays(1);

        // 2. Execute and 3. Assert
        final InvalidProfileBirthDateException thrown = Assertions.assertThrows(InvalidProfileBirthDateException.class,
                () -> userService.updateBirthDate(1L, future));
        Assertions.assertEquals(MessageKeys.USER_PROFILE_BIRTH_DATE_FUTURE, thrown.getMessageCode());

    }

    @Test
    public void testUpdatePhoneNumberWhenBlankDoesNotThrow() {
        // 1. Arrange
        final User user = User.identities(1L, "u@mail.com", "A", "B");
        Mockito.when(userDao.getUserById(1L)).thenReturn(Optional.of(user));

        // 2. Execute and 3. Assert
        Assertions.assertDoesNotThrow(() -> userService.updatePhoneNumber(1L, "   \t"));
    }

    @Test
    public void testUpdatePhoneNumberInvalidPhoneThrows() {
        // 1. Arrange
        final User user = User.identities(1L, "u@mail.com", "A", "B");
        Mockito.when(userDao.getUserById(1L)).thenReturn(Optional.of(user));

        // 2. Execute and 3. Assert
        final InvalidProfilePhoneException thrown = Assertions.assertThrows(InvalidProfilePhoneException.class,
                () -> userService.updatePhoneNumber(1L, "12a34"));
        Assertions.assertEquals(MessageKeys.USER_PROFILE_PHONE_INVALID, thrown.getMessageCode());
    }

    @Test
    public void testUpdateAboutWhenValidDoesNotThrow() {
        final User user = User.identities(1L, "u@mail.com", "A", "B");
        Mockito.when(userDao.getUserById(1L)).thenReturn(Optional.of(user));
        Assertions.assertDoesNotThrow(() -> userService.updateAbout(1L, "  About me text  "));
    }

    @Test
    public void testUpdateAboutWhenTooLongThrows() {
        final User user = User.identities(1L, "u@mail.com", "A", "B");
        Mockito.when(userDao.getUserById(1L)).thenReturn(Optional.of(user));
        final String tooLong = "a".repeat(501);

        final InvalidUserFieldLengthException thrown = Assertions.assertThrows(InvalidUserFieldLengthException.class,
                () -> userService.updateAbout(1L, tooLong));
        Assertions.assertEquals(MessageKeys.USER_PROFILE_ABOUT_TOO_LONG, thrown.getMessageCode());
    }


    @Test
    public void testUpdatePhoneNumberUserMissingThrows() {
        // 1. Arrange
        Mockito.when(userDao.getUserById(99L)).thenReturn(Optional.empty());

        // 2. Execute and 3. Assert
        Assertions.assertThrows(UserNotFoundException.class,
                () -> userService.updatePhoneNumber(99L, "+1"));
    }

    @Test
    public void testUpdateBirthDateUserMissingThrows() {
        // 1. Arrange
        Mockito.when(userDao.getUserById(99L)).thenReturn(Optional.empty());

        // 2. Execute and 3. Assert
        Assertions.assertThrows(UserNotFoundException.class,
                () -> userService.updateBirthDate(99L, LocalDate.of(2000, 1, 1)));
    }

    @Test
    public void testHasUploadedLicenseAndIdentityWhenBothPresentReturnsTrue() {
        final User user = User.builder()
                .id(1L)
                .email("a@test.com")
                .forename("A")
                .surname("B")
                .licenseFile(Mockito.mock(StoredFile.class))
                .identityFile(Mockito.mock(StoredFile.class))
                .build();

        Assertions.assertTrue(userService.hasUploadedLicenseAndIdentity(user));
    }

    @Test
    public void testHasUploadedLicenseAndIdentityWhenIdentityMissingReturnsFalse() {
        final User user = User.builder()
                .id(1L)
                .email("a@test.com")
                .forename("A")
                .surname("B")
                .licenseFile(Mockito.mock(StoredFile.class))
                .build();

        Assertions.assertFalse(userService.hasUploadedLicenseAndIdentity(user));
    }

    @Test
    public void testPromoteToAdminDoesNotThrowWhenValid() {
        // Happy-path: actor is admin, target is a regular user. The test does not assert on the
        // "sends email" or "persists role" side-effects (those would require Mockito.verify or
        // a doAnswer/slot emulation, both forbidden by the test-style rules); it only confirms
        // the call completes successfully.
        // 1. Arrange
        final User granting = User.builder()
                .id(10L)
                .email("admin@test.com")
                .forename("Grant")
                .surname("Admin")
                .userRole(UserRole.ADMIN)
                .build();
        final User target = User.builder()
                .id(20L)
                .email("user@test.com")
                .forename("Tar")
                .surname("Get")
                .userRole(UserRole.USER)
                .build();
        Mockito.when(userDao.getUserById(10L)).thenReturn(Optional.of(granting));
        Mockito.when(userDao.getUserById(20L)).thenReturn(Optional.of(target));

        // 2. Execute and 3. Assert
        Assertions.assertDoesNotThrow(() -> userService.promoteToAdmin(20L, 10L));
    }

    @Test
    public void testPromoteToAdminThrowsWhenActorNotAdmin() {
        // 1. Arrange
        final User granting = User.builder()
                .id(10L)
                .email("admin@test.com")
                .forename("Grant")
                .surname("Admin")
                .userRole(UserRole.USER)
                .build();
        final User target = User.builder()
                .id(20L)
                .email("user@test.com")
                .forename("Tar")
                .surname("Get")
                .userRole(UserRole.USER)
                .build();
        Mockito.when(userDao.getUserById(10L)).thenReturn(Optional.of(granting));
        Mockito.when(userDao.getUserById(20L)).thenReturn(Optional.of(target));

        // 2. Execute and 3. Assert
        final AdminPromoterNotAdminException thrown = Assertions.assertThrows(
                AdminPromoterNotAdminException.class,
                () -> userService.promoteToAdmin(20L, 10L));
        Assertions.assertEquals(MessageKeys.ADMIN_PROMOTE_NOT_ADMIN, thrown.getMessageCode());
    }

    @Test
    public void testPromoteToAdminThrowsWhenTargetAlreadyAdmin() {
        // 1. Arrange
        final User granting = User.builder()
                .id(10L)
                .email("admin@test.com")
                .forename("Grant")
                .surname("Admin")
                .userRole(UserRole.ADMIN)
                .build();
        final User target = User.builder()
                .id(20L)
                .email("user@test.com")
                .forename("Tar")
                .surname("Get")
                .userRole(UserRole.ADMIN)
                .build();
        Mockito.when(userDao.getUserById(10L)).thenReturn(Optional.of(granting));
        Mockito.when(userDao.getUserById(20L)).thenReturn(Optional.of(target));

        // 2. Execute and 3. Assert
        final UserAlreadyAdminException thrown = Assertions.assertThrows(
                UserAlreadyAdminException.class,
                () -> userService.promoteToAdmin(20L, 10L));
        Assertions.assertEquals(MessageKeys.ADMIN_PROMOTE_ALREADY_ADMIN, thrown.getMessageCode());
    }

    @Test
    public void testPromoteToAdminThrowsWhenTargetMissing() {
        // 1. Arrange
        final User granting = User.builder()
                .id(10L)
                .email("admin@test.com")
                .forename("Grant")
                .surname("Admin")
                .userRole(UserRole.ADMIN)
                .build();
        Mockito.when(userDao.getUserById(10L)).thenReturn(Optional.of(granting));
        Mockito.when(userDao.getUserById(20L)).thenReturn(Optional.empty());

        // 2. Execute and 3. Assert
        Assertions.assertThrows(UserNotFoundException.class,
                () -> userService.promoteToAdmin(20L, 10L));
    }

    @Test
    public void testPromoteToAdminThrowsWhenActorMissing() {
        // 1. Arrange
        Mockito.when(userDao.getUserById(10L)).thenReturn(Optional.empty());

        // 2. Execute and 3. Assert
        Assertions.assertThrows(UserNotFoundException.class,
                () -> userService.promoteToAdmin(20L, 10L));
    }

    @Test
    public void testCreateAdminUserWithEncodedPasswordSucceedsWhenActorIsAdmin() {
        // 1. Arrange
        final User granting = User.builder()
                .id(10L)
                .email("admin@test.com")
                .forename("Grant")
                .surname("Admin")
                .userRole(UserRole.ADMIN)
                .build();
        final User created = User.builder()
                .id(99L)
                .email("newadmin@test.com")
                .forename("New")
                .surname("Admin")
                .userRole(UserRole.ADMIN)
                .roleAssignedBy(10L)
                .emailValidated(true)
                .build();
        Mockito.when(userDao.getUserById(10L)).thenReturn(Optional.of(granting));
        Mockito.when(userDao.createAdminUser(
                        Mockito.eq("newadmin@test.com"),
                        Mockito.eq("New"),
                        Mockito.eq("Admin"),
                        Mockito.eq("HASH"),
                        Mockito.eq(10L)))
                .thenReturn(created);

        // 2. Execute
        final User result = userService.createAdminUserWithEncodedPassword(
                "newadmin@test.com", "New", "Admin", "HASH", 10L);

        // 3. Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(99L, result.getId());
        Assertions.assertEquals(UserRole.ADMIN, result.getUserRole());
        Assertions.assertEquals(10L, result.getRoleAssignedBy().orElse(null));
    }

    @Test
    public void testCreateAdminUserWithEncodedPasswordThrowsWhenActorMissing() {
        // 1. Arrange
        Mockito.when(userDao.getUserById(10L)).thenReturn(Optional.empty());

        // 2. Execute and 3. Assert
        Assertions.assertThrows(UserNotFoundException.class,
                () -> userService.createAdminUserWithEncodedPassword(
                        "x@test.com", "X", "Y", "HASH", 10L));
    }

    @Test
    public void testCreateAdminUserWithEncodedPasswordThrowsWhenActorNotAdmin() {
        // 1. Arrange
        final User granting = User.builder()
                .id(10L)
                .email("notadmin@test.com")
                .forename("Not")
                .surname("Admin")
                .userRole(UserRole.USER)
                .build();
        Mockito.when(userDao.getUserById(10L)).thenReturn(Optional.of(granting));

        // 2. Execute and 3. Assert
        final AdminPromoterNotAdminException thrown = Assertions.assertThrows(
                AdminPromoterNotAdminException.class,
                () -> userService.createAdminUserWithEncodedPassword(
                        "x@test.com", "X", "Y", "HASH", 10L));
        Assertions.assertEquals(MessageKeys.ADMIN_PROMOTE_NOT_ADMIN, thrown.getMessageCode());
    }

    @Test
    public void testCreateAdminUserWithEncodedPasswordReturnsAdminFromDao() {
        // 1. Arrange
        final User granting = User.builder()
                .id(10L)
                .email("admin@test.com")
                .forename("Grant")
                .surname("Admin")
                .userRole(UserRole.ADMIN)
                .build();
        final User created = User.builder()
                .id(99L)
                .email("newadmin@test.com")
                .forename("New")
                .surname("Admin")
                .userRole(UserRole.ADMIN)
                .roleAssignedBy(10L)
                .emailValidated(true)
                .build();
        Mockito.when(userDao.getUserById(10L)).thenReturn(Optional.of(granting));
        Mockito.when(userDao.createAdminUser(
                        Mockito.eq("newadmin@test.com"),
                        Mockito.eq("New"),
                        Mockito.eq("Admin"),
                        Mockito.eq("HASH"),
                        Mockito.eq(10L)))
                .thenReturn(created);

        // 2. Execute
        final User result = userService.createAdminUserWithEncodedPassword(
                "newadmin@test.com", "New", "Admin", "HASH", 10L);

        // 3. Assert: returned user reflects DAO output (admin role, pre-verified email, grantor recorded)
        Assertions.assertEquals(99L, result.getId());
        Assertions.assertEquals(UserRole.ADMIN, result.getUserRole());
        Assertions.assertEquals(10L, result.getRoleAssignedBy().orElse(null));
        Assertions.assertTrue(result.getEmailValidated().orElse(false));
    }
}
