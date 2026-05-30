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
class CarGalleryUploadPolicyTest {

    @Mock
    private Environment environment;

    @Test
    void testResolvesImageAndVideoByteLimitsFromProperties() {
        // 1.Arrange
        Mockito.when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_BYTES_PER_BINARY_MB, Integer.class))
                .thenReturn(1_048_576);
        Mockito.when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_MAX_IMAGE_MB, Long.class))
                .thenReturn(20L);
        Mockito.when(environment.getProperty(CarGalleryUploadPolicy.PROPERTY_MAX_VIDEO_MB, Long.class))
                .thenReturn(25L);

        // 2.Exercise
        final CarGalleryUploadPolicy policy = new CarGalleryUploadPolicy(environment);

        // 3.Assert
        Assertions.assertEquals(8, policy.getMaxItems());
        Assertions.assertEquals(20 * 1_048_576, policy.getMaxImageBytes());
        Assertions.assertEquals(25 * 1_048_576, policy.getMaxVideoBytes());
    }
}
