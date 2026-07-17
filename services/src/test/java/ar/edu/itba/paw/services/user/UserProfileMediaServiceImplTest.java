package ar.edu.itba.paw.services.user;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.InvalidProfileDocumentException;
import ar.edu.itba.paw.models.domain.file.Image;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.domain.user.UserDocumentType;
import ar.edu.itba.paw.models.security.UserRole;
import ar.edu.itba.paw.policy.ProfileDocumentUploadPolicy;
import ar.edu.itba.paw.policy.ProfileDocumentUploadPolicyImpl;

import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.file.StoredFileService;
@ExtendWith(MockitoExtension.class)
public class UserProfileMediaServiceImplTest {

    // Architectural rule: this service no longer touches UserDao; tests mock UserService instead,
    // which exposes the row-level FK setters used by the profile-media flows.
    @Mock
    private UserService userService;

    @Mock
    private ImageService imageService;

    @Mock
    private StoredFileService storedFileService;

    private UserProfileMediaServiceImpl profileMediaService;

    @BeforeEach
    public void setUp() {
        final Environment environment = Mockito.mock(Environment.class);
        Mockito.when(environment.getProperty("app.upload.bytes-per-binary-megabyte", Integer.class)).thenReturn(1048576);
        Mockito.when(environment.getProperty("app.upload.max-profile-document-megabytes", Long.class)).thenReturn(5L);
        final ProfileDocumentUploadPolicy profileDocumentUploadPolicy =
                new ProfileDocumentUploadPolicyImpl(environment);
        profileMediaService = new UserProfileMediaServiceImpl(
                userService, imageService, storedFileService, profileDocumentUploadPolicy);
    }

    @Test
    public void testUpdateProfilePictureWhenUserExistsDoesNotThrow() {
        final User user = User.identities(1L, "u@mail.com", "A", "B");
        Mockito.when(userService.getUserById(1L)).thenReturn(Optional.of(user));
        final Image created = Image.identified(20L, "p.png", "image/png", new byte[] {1, 2});
        Mockito.when(imageService.createImage(Mockito.eq("p.png"), Mockito.eq("image/png"), Mockito.any()))
                .thenReturn(created);

        Assertions.assertDoesNotThrow(
                () -> profileMediaService.updateProfilePicture(1L, "p.png", "image/png", new byte[] {1, 2}));
    }

    @Test
    public void testUploadProfileDocumentLeavesNonAdminPendingReview() {
        // 1. Arrange
        final byte[] pdfBytes = {0x25, 0x50, 0x44, 0x46, 0x2d}; // "%PDF-" magic header
        final User user = User.identities(1L, "u@mail.com", "A", "B");
        Mockito.when(userService.getUserById(1L)).thenReturn(Optional.of(user));
        Mockito.when(storedFileService.create(1L, "licencia.pdf", "application/pdf", pdfBytes))
                .thenReturn(StoredFile.identified(10L,
                        User.identities(1L, "u@test.com", "U", "U"),
                        "licencia.pdf", "application/pdf", pdfBytes, null));
        final AtomicBoolean validated = new AtomicBoolean(true);
        Mockito.doAnswer(invocation -> {
            validated.set(invocation.getArgument(2));
            return null;
        }).when(userService).updateLicenseDocumentFk(Mockito.eq(1L), Mockito.eq(10L), Mockito.anyBoolean());

        // 2. Act
        profileMediaService.uploadProfileDocument(
                1L, UserDocumentType.LICENSE, "licencia.pdf", "application/pdf", pdfBytes);

        // 3. Assert
        Assertions.assertFalse(validated.get());
    }

    @Test
    public void testUploadProfileDocumentAutoValidatesForAdmin() {
        // 1. Arrange
        final byte[] pdfBytes = {0x25, 0x50, 0x44, 0x46, 0x2d};
        final User admin = User.identities(1L, "admin@mail.com", "A", "B");
        admin.setUserRole(UserRole.ADMIN);
        Mockito.when(userService.getUserById(1L)).thenReturn(Optional.of(admin));
        Mockito.when(storedFileService.create(1L, "dni.pdf", "application/pdf", pdfBytes))
                .thenReturn(StoredFile.identified(11L,
                        User.identities(1L, "admin@mail.com", "A", "B"),
                        "dni.pdf", "application/pdf", pdfBytes, null));
        final AtomicBoolean validated = new AtomicBoolean(false);
        Mockito.doAnswer(invocation -> {
            validated.set(invocation.getArgument(2));
            return null;
        }).when(userService).updateIdentityDocumentFk(Mockito.eq(1L), Mockito.eq(11L), Mockito.anyBoolean());

        // 2. Act
        profileMediaService.uploadProfileDocument(
                1L, UserDocumentType.IDENTITY, "dni.pdf", "application/pdf", pdfBytes);

        // 3. Assert
        Assertions.assertTrue(validated.get());
    }

    @Test
    public void testUploadProfileDocumentReplacesExistingLicense() {
        // 1. Arrange — slot already occupied (SPA "replace" path)
        final byte[] pdfBytes = {0x25, 0x50, 0x44, 0x46, 0x2d};
        final StoredFile previous = StoredFile.identified(9L,
                User.identities(1L, "u@test.com", "U", "U"),
                "old.pdf", "application/pdf", pdfBytes, null);
        final User user = User.identities(1L, "u@mail.com", "A", "B");
        user.setLicenseFile(previous);
        Mockito.when(userService.getUserById(1L)).thenReturn(Optional.of(user));
        Mockito.when(storedFileService.create(1L, "licencia.pdf", "application/pdf", pdfBytes))
                .thenReturn(StoredFile.identified(10L,
                        User.identities(1L, "u@test.com", "U", "U"),
                        "licencia.pdf", "application/pdf", pdfBytes, null));

        // 2. Act / 3. Assert — PUT must replace, not reject with alreadyUploaded
        Assertions.assertDoesNotThrow(() -> profileMediaService.uploadProfileDocument(
                1L, UserDocumentType.LICENSE, "licencia.pdf", "application/pdf", pdfBytes));
    }

    @Test
    public void testUploadProfileDocumentRejectsInvalidType() {
        final InvalidProfileDocumentException thrown = Assertions.assertThrows(
                InvalidProfileDocumentException.class,
                () -> profileMediaService.uploadProfileDocument(
                        1L, UserDocumentType.IDENTITY, "dni.txt", "text/plain", new byte[] {1, 2}));
        Assertions.assertEquals(MessageKeys.USER_PROFILE_DOCUMENT_INVALID, thrown.getMessageCode());
    }
}
