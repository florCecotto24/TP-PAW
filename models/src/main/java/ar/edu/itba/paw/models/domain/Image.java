package ar.edu.itba.paw.models.domain;

import java.util.Arrays;

/** Stored image metadata and inline bytes (legacy path; profile and uploads may use {@link StoredFile} instead). */
public final class Image {
    private final long id;
    private final String name;
    private final String contentType;
    private final byte[] data;

    public static boolean isImageContentType(final String contentType) {
        if (contentType == null) {
            return false;
        }
        final String t = contentType.trim();
        if (t.isEmpty()) {
            return false;
        }
        return t.regionMatches(true, 0, "image/", 0, 6);
    }

    public Image(final long id, final String name, final String contentType, final byte[] data) {
        this.id = id;
        this.name = name;
        this.contentType = contentType;
        this.data = data;
    }

    public String getContentType() {
        return contentType;
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Image{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", contentType='" + contentType + '\'' +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
