package ar.edu.itba.paw.services.policy;

import ar.edu.itba.paw.services.util.UploadBinaryMegabyte;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public final class ProfileDocumentUploadPolicy {

    private final int maxBytes;
    private final long bytesPerBinaryMegabyte;

    @Autowired
    public ProfileDocumentUploadPolicy(final Environment environment) {
        this.bytesPerBinaryMegabyte = UploadBinaryMegabyte.bytesPerBinaryMegabyte(environment);
        final long raw = UploadBinaryMegabyte.maxBytesFromConfiguredMegabytes(
                environment, UploadBinaryMegabyte.PROPERTY_MAX_PROFILE_DOCUMENT_MB, 5L);
        if (raw <= 0 || raw > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    UploadBinaryMegabyte.PROPERTY_MAX_PROFILE_DOCUMENT_MB + " resolved to invalid byte count: " + raw);
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
