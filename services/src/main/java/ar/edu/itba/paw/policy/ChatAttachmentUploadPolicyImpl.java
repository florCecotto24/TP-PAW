package ar.edu.itba.paw.policy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.util.UploadBinaryMegabyte;

/**
 * Reads {@code app.upload.max-chat-attachment-megabytes} and {@code app.upload.bytes-per-binary-megabyte}
 * to back the {@link ChatAttachmentUploadPolicy} contract.
 */
@Component
public final class ChatAttachmentUploadPolicyImpl implements ChatAttachmentUploadPolicy {

    private final int maxBytes;
    private final long bytesPerBinaryMegabyte;

    @Autowired
    public ChatAttachmentUploadPolicyImpl(final Environment environment) {
        this.bytesPerBinaryMegabyte = UploadBinaryMegabyte.bytesPerBinaryMegabyte(environment);
        final long raw = UploadBinaryMegabyte.maxBytesFromConfiguredMegabytes(
                environment, UploadBinaryMegabyte.PROPERTY_MAX_CHAT_ATTACHMENT_MB, 25L);
        if (raw <= 0 || raw > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    UploadBinaryMegabyte.PROPERTY_MAX_CHAT_ATTACHMENT_MB + " resolved to invalid byte count: " + raw);
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
