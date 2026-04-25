package ar.edu.itba.paw.models;

import java.time.OffsetDateTime;

public final class StoredFile {
    private final long id;
    private final long uploaderUserId;
    private final String fileName;
    private final String contentType;
    private final byte[] data;
    private final OffsetDateTime createdAt;

    public StoredFile(
            final long id,
            final long uploaderUserId,
            final String fileName,
            final String contentType,
            final byte[] data,
            final OffsetDateTime createdAt) {
        this.id = id;
        this.uploaderUserId = uploaderUserId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public long getUploaderUserId() {
        return uploaderUserId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public static boolean isAllowedPaymentReceiptContentType(final String contentType) {
        if (contentType == null) {
            return false;
        }
        final String t = contentType.trim().toLowerCase();
        return t.startsWith("image/")
                || "application/pdf".equals(t);
    }
}
