package ar.edu.itba.paw.models.dto.file;

import java.util.Objects;

/**
 * Inline bytes plus the metadata an HTTP download endpoint needs (filename, content type).
 *
 * Returned by the file-byte service helpers so controllers can build the {@code ResponseEntity}
 * from a detached value object. Keeps the JPA entity ({@link ar.edu.itba.paw.models.domain.Image}
 * / {@link ar.edu.itba.paw.models.domain.StoredFile}) inside the persistence + service layers,
 * where the {@code byte_array} access lives, instead of leaking it across HTTP code (issue #16).
 *
 * Filename is optional — only {@link ar.edu.itba.paw.models.domain.StoredFile} rows carry one;
 * legacy {@link ar.edu.itba.paw.models.domain.Image} rows expose only {@code name} which is not
 * surfaced as a download header today.
 */
public final class BinaryContent {

    private final byte[] bytes;
    private final String contentType;
    private final String fileName;

    public BinaryContent(final byte[] bytes, final String contentType, final String fileName) {
        this.bytes = Objects.requireNonNull(bytes, "bytes");
        this.contentType = contentType;
        this.fileName = fileName;
    }

    public BinaryContent(final byte[] bytes, final String contentType) {
        this(bytes, contentType, null);
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFileName() {
        return fileName;
    }

    public int getSize() {
        return bytes.length;
    }
}
