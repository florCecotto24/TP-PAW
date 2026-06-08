package ar.edu.itba.paw.policy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import ar.edu.itba.paw.util.UploadBinaryMegabyte;

@ExtendWith(MockitoExtension.class)
class PaymentReceiptUploadPolicyTest {

    @Mock
    private Environment environment;

    @Test
    void testConstructorUsesDefaultWhenPropertyMissing() {
        // 1.Arrange / 2.Act
        final PaymentReceiptUploadPolicy policy = new PaymentReceiptUploadPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(5 * UploadBinaryMegabyte.DEFAULT_BYTES_PER_BINARY_MB, policy.getMaxBytes());
        Assertions.assertEquals(5, policy.getMaxMegabytesRoundedUp());
    }

    @Test
    void testConstructorUsesConfiguredMegabytes() {
        // 1.Arrange
        // lenient(): the SUT also reads PROPERTY_BYTES_PER_BINARY_MB (Integer); we leave it unstubbed
        // so the default 1 MiB unit applies. Without lenient, strict-stubs flags the megabytes stub
        // as a "potential stubbing problem" on that other call.
        Mockito.lenient().when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_MAX_PAYMENT_RECEIPT_MB, Long.class))
                .thenReturn(10L);

        // 2.Act
        final PaymentReceiptUploadPolicy policy = new PaymentReceiptUploadPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(10 * UploadBinaryMegabyte.DEFAULT_BYTES_PER_BINARY_MB, policy.getMaxBytes());
        Assertions.assertEquals(10, policy.getMaxMegabytesRoundedUp());
    }

    @Test
    void testConstructorThrowsWhenResolvedSizeOverflowsInteger() {
        // 1.Arrange
        // lenient(): same rationale as testConstructorUsesConfiguredMegabytes.
        Mockito.lenient().when(environment.getProperty(UploadBinaryMegabyte.PROPERTY_MAX_PAYMENT_RECEIPT_MB, Long.class))
                .thenReturn(5_000L);

        // 2.Act / 3.Assert
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new PaymentReceiptUploadPolicyImpl(environment));
    }
}
