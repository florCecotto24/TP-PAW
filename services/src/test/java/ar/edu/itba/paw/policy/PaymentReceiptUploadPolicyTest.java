package ar.edu.itba.paw.policy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;

import ar.edu.itba.paw.util.UploadBinaryMegabyte;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentReceiptUploadPolicyTest {

    @Mock
    private Environment environment;

    @Test
    void testConstructorUsesDefaultWhenPropertyMissing() {
        // 1.Arrange / 2.Exercise
        final PaymentReceiptUploadPolicy policy = new PaymentReceiptUploadPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(5 * UploadBinaryMegabyte.DEFAULT_BYTES_PER_BINARY_MB, policy.getMaxBytes());
        Assertions.assertEquals(5, policy.getMaxMegabytesRoundedUp());
    }

    @Test
    void testConstructorUsesConfiguredMegabytes() {
        // 1.Arrange
        Mockito.when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_MAX_PAYMENT_RECEIPT_MB, Long.class))
                .thenReturn(10L);

        // 2.Exercise
        final PaymentReceiptUploadPolicy policy = new PaymentReceiptUploadPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(10 * UploadBinaryMegabyte.DEFAULT_BYTES_PER_BINARY_MB, policy.getMaxBytes());
        Assertions.assertEquals(10, policy.getMaxMegabytesRoundedUp());
    }

    @Test
    void testConstructorThrowsWhenResolvedSizeOverflowsInteger() {
        // 1.Arrange
        Mockito.when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_MAX_PAYMENT_RECEIPT_MB, Long.class))
                .thenReturn(5_000L);

        // 2.Exercise / 3.Assert
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new PaymentReceiptUploadPolicyImpl(environment));
    }
}
