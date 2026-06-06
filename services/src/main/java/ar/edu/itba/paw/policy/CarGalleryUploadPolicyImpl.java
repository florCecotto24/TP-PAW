package ar.edu.itba.paw.policy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.util.UploadBinaryMegabyte;

/**
 * Reads {@code app.upload.max-image-megabytes} and {@code app.upload.max-car-gallery-video-megabytes}
 * to back the {@link CarGalleryUploadPolicy} contract.
 */
@Component
public final class CarGalleryUploadPolicyImpl implements CarGalleryUploadPolicy {

    public static final String PROPERTY_MAX_VIDEO_MB = "app.upload.max-car-gallery-video-megabytes";

    private final int maxImageBytes;
    private final int maxVideoBytes;
    private final long bytesPerBinaryMegabyte;

    @Autowired
    public CarGalleryUploadPolicyImpl(final Environment environment) {
        this.bytesPerBinaryMegabyte = UploadBinaryMegabyte.bytesPerBinaryMegabyte(environment);
        this.maxImageBytes = toPositiveInt(UploadBinaryMegabyte.maxBytesFromConfiguredMegabytes(
                environment, UploadBinaryMegabyte.PROPERTY_MAX_IMAGE_MB, 20L));
        this.maxVideoBytes = toPositiveInt(UploadBinaryMegabyte.maxBytesFromConfiguredMegabytes(
                environment, PROPERTY_MAX_VIDEO_MB, 25L));
    }

    @Override
    public int getMaxItems() {
        return MAX_ITEMS;
    }

    @Override
    public int getMaxImageBytes() {
        return maxImageBytes;
    }

    @Override
    public int getMaxVideoBytes() {
        return maxVideoBytes;
    }

    @Override
    public int getMaxImageMegabytesRoundedUp() {
        return megabytesRoundedUp(maxImageBytes);
    }

    @Override
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
