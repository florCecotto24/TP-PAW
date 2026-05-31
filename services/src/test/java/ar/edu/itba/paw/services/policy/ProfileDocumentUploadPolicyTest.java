package ar.edu.itba.paw.services.policy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;

import ar.edu.itba.paw.services.util.UploadBinaryMegabyte;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileDocumentUploadPolicyTest {

    @Mock
    private Environment environment;

    @Test
    void testConstructorUsesDefaultMegabytesWhenPropertyMissing() {
        // 1.Arrange / 2.Exercise
        final ProfileDocumentUploadPolicy policy = new ProfileDocumentUploadPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(5 * UploadBinaryMegabyte.DEFAULT_BYTES_PER_BINARY_MB, policy.getMaxBytes());
        Assertions.assertEquals(5, policy.getMaxMegabytesRoundedUp());
    }

    @Test
    void testConstructorUsesConfiguredMegabytesAndCustomBinaryUnit() {
        // 1.Arrange
        Mockito.when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_MAX_PROFILE_DOCUMENT_MB, Long.class))
                .thenReturn(2L);
        Mockito.when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_BYTES_PER_BINARY_MB, Integer.class))
                .thenReturn(1000);

        // 2.Exercise
        final ProfileDocumentUploadPolicy policy = new ProfileDocumentUploadPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(2_000, policy.getMaxBytes());
        Assertions.assertEquals(2, policy.getMaxMegabytesRoundedUp());
    }

    @Test
    void testGetMaxMegabytesRoundedUpRoundsPartialValues() {
        // 1.Arrange
        Mockito.when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_BYTES_PER_BINARY_MB, Integer.class))
                .thenReturn(1024);
        Mockito.when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_MAX_PROFILE_DOCUMENT_MB, Long.class))
                .thenReturn(3L);

        // 2.Exercise
        final ProfileDocumentUploadPolicy policy = new ProfileDocumentUploadPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(3 * 1024, policy.getMaxBytes());
        Assertions.assertEquals(3, policy.getMaxMegabytesRoundedUp());
    }

    @Test
    void testConstructorThrowsWhenResolvedSizeOverflowsInteger() {
        // 1.Arrange
        Mockito.when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_MAX_PROFILE_DOCUMENT_MB, Long.class))
                .thenReturn(5_000L);

        // 2.Exercise / 3.Assert
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ProfileDocumentUploadPolicyImpl(environment));
    }
}
