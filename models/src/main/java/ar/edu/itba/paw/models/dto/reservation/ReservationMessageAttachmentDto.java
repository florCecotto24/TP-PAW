package ar.edu.itba.paw.models.dto.reservation;

import ar.edu.itba.paw.models.util.media.ChatAttachmentKind;

/** Attachment metadata for a reservation chat message (no binary payload). */
public final class ReservationMessageAttachmentDto {

    private final long fileId;
    private final String fileName;
    private final String contentType;
    private final long sizeBytes;
    private final ChatAttachmentKind kind;
    private final String url;

    public ReservationMessageAttachmentDto(
            final long fileId,
            final String fileName,
            final String contentType,
            final long sizeBytes,
            final ChatAttachmentKind kind,
            final String url) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.kind = kind;
        this.url = url;
    }

    public long getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public ChatAttachmentKind getKind() {
        return kind;
    }

    public String getUrl() {
        return url;
    }
}
