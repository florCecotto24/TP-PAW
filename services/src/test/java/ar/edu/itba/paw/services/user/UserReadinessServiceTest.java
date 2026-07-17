package ar.edu.itba.paw.services.user;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.user.User;

class UserReadinessServiceTest {

    private final UserReadinessService readinessService = new UserReadinessServiceImpl();

    @Test
    void testHasUploadedLicenseAndIdentityWhenBothPresentReturnsTrue() {
        // 1.Arrange
        final User user = User.builder()
                .id(1L)
                .email("a@test.com")
                .forename("A")
                .surname("B")
                .licenseFile(Mockito.mock(StoredFile.class))
                .identityFile(Mockito.mock(StoredFile.class))
                .build();

        // 2.Act / 3.Assert
        Assertions.assertTrue(readinessService.hasUploadedLicenseAndIdentity(user));
    }

    @Test
    void testHasUploadedLicenseAndIdentityWhenIdentityMissingReturnsFalse() {
        // 1.Arrange
        final User user = User.builder()
                .id(1L)
                .email("a@test.com")
                .forename("A")
                .surname("B")
                .licenseFile(Mockito.mock(StoredFile.class))
                .build();

        // 2.Act / 3.Assert
        Assertions.assertFalse(readinessService.hasUploadedLicenseAndIdentity(user));
    }
}
