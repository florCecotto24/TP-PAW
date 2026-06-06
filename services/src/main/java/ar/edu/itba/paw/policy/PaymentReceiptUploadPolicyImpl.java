package ar.edu.itba.paw.policy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.util.UploadBinaryMegabyte;

/**
 * Reads {@code app.upload.max-payment-receipt-megabytes} and {@code app.upload.bytes-per-binary-megabyte}
 * to back the {@link PaymentReceiptUploadPolicy} contract.
 */
@Component
public final class PaymentReceiptUploadPolicyImpl implements PaymentReceiptUploadPolicy {

    private final int maxBytes;
    private final long bytesPerBinaryMegabyte;

    @Autowired
    public PaymentReceiptUploadPolicyImpl(final Environment environment) {
        this.bytesPerBinaryMegabyte = UploadBinaryMegabyte.bytesPerBinaryMegabyte(environment);
        final long raw = UploadBinaryMegabyte.maxBytesFromConfiguredMegabytes(
                environment, UploadBinaryMegabyte.PROPERTY_MAX_PAYMENT_RECEIPT_MB, 5L);
        if (raw <= 0 || raw > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    UploadBinaryMegabyte.PROPERTY_MAX_PAYMENT_RECEIPT_MB + " resolved to invalid byte count: " + raw);
        }
        this.maxBytes = (int) raw;
    }

    @Override
    public int getMaxBytes() {
        return maxBytes;
    }

    @Override
    public int getMaxMegabytesRoundedUp() {
        return (int) ((maxBytes + bytesPerBinaryMegabyte - 1) / bytesPerBinaryMegabyte);
    }
}
