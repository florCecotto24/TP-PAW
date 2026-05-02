package ar.edu.itba.paw.services.policy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.util.UploadBinaryMegabyte;

/**
 * Max byte size for reservation payment receipt uploads, from {@code app.upload.max-payment-receipt-megabytes}
 * and {@code app.upload.bytes-per-binary-megabyte}.
 */
@Component
public final class PaymentReceiptUploadPolicy {

    private final int maxBytes;
    private final long bytesPerBinaryMegabyte;

    @Autowired
    public PaymentReceiptUploadPolicy(final Environment environment) {
        this.bytesPerBinaryMegabyte = UploadBinaryMegabyte.bytesPerBinaryMegabyte(environment);
        final long raw = UploadBinaryMegabyte.maxBytesFromConfiguredMegabytes(
                environment, UploadBinaryMegabyte.PROPERTY_MAX_PAYMENT_RECEIPT_MB, 5L);
        if (raw <= 0 || raw > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    UploadBinaryMegabyte.PROPERTY_MAX_PAYMENT_RECEIPT_MB + " resolved to invalid byte count: " + raw);
        }
        this.maxBytes = (int) raw;
    }

    public int getMaxBytes() {
        return maxBytes;
    }

    public int getMaxMegabytesRoundedUp() {
        return (int) ((maxBytes + bytesPerBinaryMegabyte - 1) / bytesPerBinaryMegabyte);
    }
}
