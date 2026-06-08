package ar.edu.itba.paw.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
class UploadBinaryMegabyteTest {

    @Mock
    private Environment environment;

    @Test
    void testBytesPerBinaryMegabyteFallsBackToOneMebibyte() {
        // 1.Arrange / 2.Act
        final long bytes = UploadBinaryMegabyte.bytesPerBinaryMegabyte(environment);

        // 3.Assert
        Assertions.assertEquals(UploadBinaryMegabyte.DEFAULT_BYTES_PER_BINARY_MB, bytes);
        Assertions.assertEquals(1024 * 1024, bytes);
    }

    @Test
    void testBytesPerBinaryMegabyteFallsBackWhenPropertyZeroOrNegative() {
        // 1.Arrange
        Mockito.when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_BYTES_PER_BINARY_MB, Integer.class))
                .thenReturn(0);

        // 2.Act
        final long bytes = UploadBinaryMegabyte.bytesPerBinaryMegabyte(environment);

        // 3.Assert
        Assertions.assertEquals(UploadBinaryMegabyte.DEFAULT_BYTES_PER_BINARY_MB, bytes);
    }

    @Test
    void testBytesPerBinaryMegabyteUsesPositivePropertyValue() {
        // 1.Arrange
        Mockito.when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_BYTES_PER_BINARY_MB, Integer.class))
                .thenReturn(1024);

        // 2.Act
        final long bytes = UploadBinaryMegabyte.bytesPerBinaryMegabyte(environment);

        // 3.Assert
        Assertions.assertEquals(1024, bytes);
    }

    @Test
    void testMaxBytesFromConfiguredMegabytesUsesConfiguredValue() {
        // 1.Arrange
        // lenient(): the SUT also calls getProperty(BYTES_PER_BINARY_MB, Integer.class) which we
        // intentionally leave unstubbed so that the default 1 MiB unit applies — without lenient,
        // strict-stubs flags the mb-key stub as a "potential stubbing problem" on the other call.
        Mockito.lenient().when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_MAX_IMAGE_MB, Long.class))
                .thenReturn(3L);

        // 2.Act
        final long bytes = UploadBinaryMegabyte.maxBytesFromConfiguredMegabytes(
                environment, UploadBinaryMegabyte.PROPERTY_MAX_IMAGE_MB, 5L);

        // 3.Assert
        Assertions.assertEquals(3L * UploadBinaryMegabyte.DEFAULT_BYTES_PER_BINARY_MB, bytes);
    }

    @Test
    void testMaxBytesFromConfiguredMegabytesAppliesDefaultWhenPropertyMissing() {
        // 1.Arrange / 2.Act
        final long bytes = UploadBinaryMegabyte.maxBytesFromConfiguredMegabytes(
                environment, UploadBinaryMegabyte.PROPERTY_MAX_IMAGE_MB, 5L);

        // 3.Assert
        Assertions.assertEquals(5L * UploadBinaryMegabyte.DEFAULT_BYTES_PER_BINARY_MB, bytes);
    }

    @Test
    void testMaxBytesFromConfiguredMegabytesAppliesDefaultWhenPropertyZeroOrNegative() {
        // 1.Arrange
        // lenient(): see testMaxBytesFromConfiguredMegabytesUsesConfiguredValue — same rationale.
        Mockito.lenient().when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_MAX_IMAGE_MB, Long.class))
                .thenReturn(0L);

        // 2.Act
        final long bytes = UploadBinaryMegabyte.maxBytesFromConfiguredMegabytes(
                environment, UploadBinaryMegabyte.PROPERTY_MAX_IMAGE_MB, 7L);

        // 3.Assert
        Assertions.assertEquals(7L * UploadBinaryMegabyte.DEFAULT_BYTES_PER_BINARY_MB, bytes);
    }

    @Test
    void testMaxBytesFromConfiguredMegabytesThrowsArithmeticExceptionOnOverflow() {
        // 1.Arrange
        final long huge = Long.MAX_VALUE / 2;
        // lenient(): see testMaxBytesFromConfiguredMegabytesUsesConfiguredValue — same rationale.
        Mockito.lenient().when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_MAX_IMAGE_MB, Long.class))
                .thenReturn(huge);

        // 2.Act / 3.Assert
        Assertions.assertThrows(ArithmeticException.class,
                () -> UploadBinaryMegabyte.maxBytesFromConfiguredMegabytes(
                        environment, UploadBinaryMegabyte.PROPERTY_MAX_IMAGE_MB, 5L));
    }
}
