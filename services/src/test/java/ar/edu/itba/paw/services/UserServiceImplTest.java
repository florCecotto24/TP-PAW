package ar.edu.itba.paw.services;

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
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.EmailAlreadyExistsException;
import ar.edu.itba.paw.exception.user.InvalidProfileBirthDateException;
import ar.edu.itba.paw.exception.user.InvalidProfileDocumentException;
import ar.edu.itba.paw.exception.user.InvalidProfilePhoneException;
import ar.edu.itba.paw.exception.user.InvalidUserFieldLengthException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.domain.UserDocumentType;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.services.policy.ProfileDocumentUploadPolicy;
import ar.edu.itba.paw.services.policy.UserValidationPolicy;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UserServiceImplTest {

    @Mock
    private UserDao userDao;

    @Mock
    private ImageService imageService;

    @Mock
    private EmailService emailService;

    @Mock
    private StoredFileService storedFileService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private ListingService listingService;

    private UserServiceImpl userService;
    private ProfileDocumentUploadPolicy profileDocumentUploadPolicy;

    @BeforeEach
    public void setUp() {
        final Environment environment = Mockito.mock(Environment.class);
        Mockito.when(environment.getProperty("app.upload.bytes-per-binary-megabyte", Integer.class)).thenReturn(1048576);
        Mockito.when(environment.getProperty("app.upload.max-profile-document-megabytes", Long.class)).thenReturn(5L);
        profileDocumentUploadPolicy = new ProfileDocumentUploadPolicy(environment);
        userService = new UserServiceImpl(
                userDao,
                imageService,
                storedFileService,
                emailService,
                passwordEncoder,
                profileDocumentUploadPolicy,
                UserValidationPolicy.fromValidatedConfiguration(8, 72, 50, 50, 20, 500, "^[0-9+]+$"),
                emailVerificationService,
                listingService);
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
        final LocalDate today = LocalDate.now(AvailabilityPeriod.WALL_ZONE);

        // 2. Execute and 3. Assert
        Assertions.assertDoesNotThrow(() -> userService.updateBirthDate(1L, today));
    }

    @Test
    public void testUpdateBirthDateFutureThrows() {
        // 1. Arrange
        final User user = User.identities(1L, "u@mail.com", "A", "B");
        Mockito.when(userDao.getUserById(1L)).thenReturn(Optional.of(user));
        final LocalDate future = LocalDate.now(AvailabilityPeriod.WALL_ZONE).plusDays(1);

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
    public void testUpdateProfilePictureWhenUserExistsDoesNotThrow() {
        // 1. Arrange
        final User user = User.builder()
                .id(1L)
                .email("u@mail.com")
                .forename("A")
                .surname("B")
                .profilePictureId(9L)
                .build();
        Mockito.when(userDao.getUserById(1L)).thenReturn(Optional.of(user));
        final Image created = new Image(20L, "p.png", "image/png", new byte[] {1, 2});
        Mockito.when(imageService.createImage(Mockito.eq("p.png"), Mockito.eq("image/png"), Mockito.any()))
                .thenReturn(created);

        // 2. Execute and 3. Assert
        Assertions.assertDoesNotThrow(
                () -> userService.updateProfilePicture(1L, "p.png", "image/png", new byte[] {1, 2}));
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
    public void testUploadValidatedProfileDocumentStoresLicenseAndMarksValidated() {
        final User user = User.identities(1L, "u@mail.com", "A", "B");
        Mockito.when(userDao.getUserById(1L)).thenReturn(Optional.of(user));
        Mockito.when(storedFileService.create(1L, "licencia.pdf", "application/pdf", new byte[] {1, 2, 3}))
                .thenReturn(new StoredFile(10L, 1L, "licencia.pdf", "application/pdf", new byte[] {1, 2, 3}, null));

        Assertions.assertDoesNotThrow(() -> userService.uploadValidatedProfileDocument(
                1L, UserDocumentType.LICENSE, "licencia.pdf", "application/pdf", new byte[] {1, 2, 3}));

    }

    @Test
    public void testUploadValidatedProfileDocumentRejectsInvalidType() {
        final User user = User.identities(1L, "u@mail.com", "A", "B");
        Mockito.when(userDao.getUserById(1L)).thenReturn(Optional.of(user));

        final InvalidProfileDocumentException thrown = Assertions.assertThrows(
                InvalidProfileDocumentException.class,
                () -> userService.uploadValidatedProfileDocument(
                        1L, UserDocumentType.IDENTITY, "dni.txt", "text/plain", new byte[] {1, 2}));
        Assertions.assertEquals(MessageKeys.USER_PROFILE_DOCUMENT_INVALID, thrown.getMessageCode());
    }

    @Test
    public void testFindOwnerCbuForListingWhenOwnerHasCbuReturnsValue() {
        final long listingId = 2L;
        final User owner = User.builder()
                .id(9L)
                .email("o@example.com")
                .forename("O")
                .surname("Owner")
                .cbu("0170200203000008777719")
                .build();
        Mockito.when(userDao.getListingOwner(listingId)).thenReturn(Optional.of(owner));
        Mockito.when(userDao.getUserById(9L)).thenReturn(Optional.of(owner));

        final Optional<String> result = userService.findOwnerCbuForListing(listingId);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("0170200203000008777719", result.get());
    }

    @Test
    public void testFindOwnerCbuForListingWhenNoOwnerReturnsEmpty() {
        Mockito.when(userDao.getListingOwner(2L)).thenReturn(Optional.empty());

        final Optional<String> result = userService.findOwnerCbuForListing(2L);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testFindOwnerCbuForListingWhenCbuMissingReturnsEmpty() {
        final User owner = User.identities(9L, "o@example.com", "O", "Owner");
        Mockito.when(userDao.getListingOwner(2L)).thenReturn(Optional.of(owner));
        Mockito.when(userDao.getUserById(9L)).thenReturn(Optional.of(owner));

        final Optional<String> result = userService.findOwnerCbuForListing(2L);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testHasUploadedLicenseAndIdentityWhenBothPresentReturnsTrue() {
        final User user = User.builder()
                .id(1L)
                .email("a@test.com")
                .forename("A")
                .surname("B")
                .licenseFileId(10L)
                .identityFileId(20L)
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
                .licenseFileId(10L)
                .build();

        Assertions.assertFalse(userService.hasUploadedLicenseAndIdentity(user));
    }
}
