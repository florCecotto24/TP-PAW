package ar.edu.itba.paw.services.util;

import org.springframework.core.env.Environment;

/**
 * Resolves upload limits expressed in binary megabytes (MiB): {@code megabytes × bytes-per-binary-megabyte}.
 * Default unit size is 1048576 bytes (1024²), matching typical JVM / OS file size semantics.
 */
public final class UploadBinaryMegabyte {

    private UploadBinaryMegabyte() {
    }

    public static final String PROPERTY_BYTES_PER_BINARY_MB = "app.upload.bytes-per-binary-megabyte";
    public static final String PROPERTY_MAX_IMAGE_MB = "app.upload.max-image-megabytes";
    public static final String PROPERTY_MAX_PAYMENT_RECEIPT_MB = "app.upload.max-payment-receipt-megabytes";
    public static final String PROPERTY_MAX_PROFILE_DOCUMENT_MB = "app.upload.max-profile-document-megabytes";
    public static final String PROPERTY_MAX_CHAT_ATTACHMENT_MB = "app.upload.max-chat-attachment-megabytes";
    public static final String PROPERTY_MAX_CAR_GALLERY_VIDEO_MB = "app.upload.max-car-gallery-video-megabytes";
    public static final String PROPERTY_MAX_MULTIPART_REQUEST_MB = "app.upload.max-multipart-request-megabytes";

    public static final int DEFAULT_BYTES_PER_BINARY_MB = 1024 * 1024;

    public static long bytesPerBinaryMegabyte(final Environment environment) {
        final Integer v = environment.getProperty(PROPERTY_BYTES_PER_BINARY_MB, Integer.class);
        if (v != null && v > 0) {
            return v.longValue();
        }
        return DEFAULT_BYTES_PER_BINARY_MB;
    }

    /**
     * @param megabytesPropertyKey e.g. {@link #PROPERTY_MAX_IMAGE_MB}
     * @param defaultMegabytes     used when the property is missing or non-positive
     */
    public static long maxBytesFromConfiguredMegabytes(
            final Environment environment,
            final String megabytesPropertyKey,
            final long defaultMegabytes) {
        final long unit = bytesPerBinaryMegabyte(environment);
        final Long mb = environment.getProperty(megabytesPropertyKey, Long.class);
        final long n = (mb != null && mb > 0) ? mb : defaultMegabytes;
        return Math.multiplyExact(n, unit);
    }
}
