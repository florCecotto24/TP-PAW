package ar.edu.itba.paw.services.policy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.util.UploadBinaryMegabyte;

/**
 * Limits for car publish gallery uploads: up to 8 mixed photos/videos with per-type byte caps from
 * {@code app.upload.max-image-megabytes} and {@code app.upload.max-car-gallery-video-megabytes}.
 */
@Component
public final class CarGalleryUploadPolicy {

    public static final int MAX_ITEMS = 8;

    public static final String PROPERTY_MAX_VIDEO_MB = "app.upload.max-car-gallery-video-megabytes";

    private final int maxImageBytes;
    private final int maxVideoBytes;
    private final long bytesPerBinaryMegabyte;

    @Autowired
    public CarGalleryUploadPolicy(final Environment environment) {
        this.bytesPerBinaryMegabyte = UploadBinaryMegabyte.bytesPerBinaryMegabyte(environment);
        this.maxImageBytes = toPositiveInt(UploadBinaryMegabyte.maxBytesFromConfiguredMegabytes(
                environment, UploadBinaryMegabyte.PROPERTY_MAX_IMAGE_MB, 20L));
        this.maxVideoBytes = toPositiveInt(UploadBinaryMegabyte.maxBytesFromConfiguredMegabytes(
                environment, PROPERTY_MAX_VIDEO_MB, 25L));
    }

    public int getMaxItems() {
        return MAX_ITEMS;
    }

    public int getMaxImageBytes() {
        return maxImageBytes;
    }

    public int getMaxVideoBytes() {
        return maxVideoBytes;
    }

    public int getMaxImageMegabytesRoundedUp() {
        return megabytesRoundedUp(maxImageBytes);
    }

    public int getMaxVideoMegabytesRoundedUp() {
        return megabytesRoundedUp(maxVideoBytes);
    }

    private int megabytesRoundedUp(final int bytes) {
        return (int) ((bytes + bytesPerBinaryMegabyte - 1) / bytesPerBinaryMegabyte);
    }

    private static int toPositiveInt(final long raw) {
        if (raw <= 0 || raw > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid upload byte limit: " + raw);
        }
        return (int) raw;
    }
}
