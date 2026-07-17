package ar.edu.itba.paw.dto;

import java.util.Arrays;
import java.util.Objects;

/**
 * Binary image upload for service-layer APIs.
 *
 * The constructor takes a defensive copy of {@code data}; callers may retain and mutate their
 * original array without affecting this DTO.
 */
public final class ImageUpload {

    private final String filename;
    private final String contentType;
    private final byte[] data;

    public ImageUpload(final String filename, final String contentType, final byte[] data) {
        this.filename = filename;
        this.contentType = contentType;
        final byte[] source = Objects.requireNonNullElse(data, new byte[0]);
        this.data = Arrays.copyOf(source, source.length);
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    /** Defensive copy so callers cannot mutate the stored payload. */
    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }
}
