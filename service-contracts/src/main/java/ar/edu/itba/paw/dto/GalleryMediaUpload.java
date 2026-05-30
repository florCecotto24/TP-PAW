package ar.edu.itba.paw.dto;

import java.util.Objects;

/** One photo or video uploaded as part of the car publish gallery. */
public final class GalleryMediaUpload {

    private final String filename;
    private final String contentType;
    private final byte[] data;

    public GalleryMediaUpload(final String filename, final String contentType, final byte[] data) {
        this.filename = filename;
        this.contentType = contentType;
        this.data = Objects.requireNonNullElse(data, new byte[0]);
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }
}
