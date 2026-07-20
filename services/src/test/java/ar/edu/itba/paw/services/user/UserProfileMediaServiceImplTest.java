package ar.edu.itba.paw.services.user;

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
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.user.UserDocumentType;
import ar.edu.itba.paw.policy.ProfileDocumentUploadPolicy;
import ar.edu.itba.paw.policy.ProfileDocumentUploadPolicyImpl;

import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.file.StoredFileService;

import java.util.Optional;

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
    public void testUploadProfileDocumentRejectsInvalidType() {
        // 1. Arrange — plain-text payload is not an accepted profile-document content type

        // 2. Act
        final InvalidProfileDocumentException thrown = Assertions.assertThrows(
                InvalidProfileDocumentException.class,
                () -> profileMediaService.uploadProfileDocument(
                        1L, UserDocumentType.IDENTITY, "dni.txt", "text/plain", new byte[] {1, 2}));

        // 3. Assert
        Assertions.assertEquals(MessageKeys.USER_PROFILE_DOCUMENT_INVALID, thrown.getMessageCode());
    }

    @Test
    public void testUploadProfileDocumentThrowsWhenUserMissing() {
        // 1. Arrange — valid PDF passes upload validation, but the user row does not exist
        final byte[] pdfBytes = {0x25, 0x50, 0x44, 0x46, 0x2d}; // "%PDF-" magic header
        Mockito.when(userService.getUserById(1L)).thenReturn(Optional.empty());

        // 2. Act and 3. Assert
        Assertions.assertThrows(
                UserNotFoundException.class,
                () -> profileMediaService.uploadProfileDocument(
                        1L, UserDocumentType.LICENSE, "licencia.pdf", "application/pdf", pdfBytes));
    }
}
