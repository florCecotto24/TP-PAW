package ar.edu.itba.paw.webapp.dto.rest;

import ar.edu.itba.paw.models.dto.reservation.ReservationMessageAttachmentDto;
import ar.edu.itba.paw.models.util.media.ChatAttachmentKind;

/**
 * Non-binary attachment metadata on {@link MessageDto} (filename, MIME, size, display kind).
 */
public final class MessageAttachmentDto {

    private String fileName;
    private String contentType;
    private long sizeBytes;
    private ChatAttachmentKind kind;

    public MessageAttachmentDto() {
    }

    public static MessageAttachmentDto fromInternal(final ReservationMessageAttachmentDto attachment) {
        if (attachment == null) {
            return null;
        }
        final MessageAttachmentDto dto = new MessageAttachmentDto();
        dto.fileName = attachment.getFileName();
        dto.contentType = attachment.getContentType();
        dto.sizeBytes = attachment.getSizeBytes();
        dto.kind = attachment.getKind();
        return dto;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(final long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public ChatAttachmentKind getKind() {
        return kind;
    }

    public void setKind(final ChatAttachmentKind kind) {
        this.kind = kind;
    }
}
