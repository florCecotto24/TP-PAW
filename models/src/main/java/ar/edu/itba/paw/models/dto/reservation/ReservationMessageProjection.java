package ar.edu.itba.paw.models.dto.reservation;

import java.time.OffsetDateTime;

/**
 * Metadata-only chat message row.
 *
 * Used by chat list/poll endpoints so attachment metadata can be rendered without loading the
 * StoredFile byte array.
 */
public final class ReservationMessageProjection {

    private final long id;
    private final long reservationId;
    private final long senderUserId;
    private final String senderForename;
    private final String senderSurname;
    private final String body;
    private final OffsetDateTime createdAt;
    private final boolean seen;
    private final Long attachmentFileId;
    private final String attachmentFileName;
    private final String attachmentContentType;
    private final Long attachmentSizeBytes;

    public ReservationMessageProjection(
            final long id,
            final long reservationId,
            final long senderUserId,
            final String senderForename,
            final String senderSurname,
            final String body,
            final OffsetDateTime createdAt,
            final boolean seen,
            final Long attachmentFileId,
            final String attachmentFileName,
            final String attachmentContentType,
            final Long attachmentSizeBytes) {
        this.id = id;
        this.reservationId = reservationId;
        this.senderUserId = senderUserId;
        this.senderForename = senderForename;
        this.senderSurname = senderSurname;
        this.body = body;
        this.createdAt = createdAt;
        this.seen = seen;
        this.attachmentFileId = attachmentFileId;
        this.attachmentFileName = attachmentFileName;
        this.attachmentContentType = attachmentContentType;
        this.attachmentSizeBytes = attachmentSizeBytes;
    }

    public long getId() {
        return id;
    }

    public long getReservationId() {
        return reservationId;
    }

    public long getSenderUserId() {
        return senderUserId;
    }

    public String getSenderForename() {
        return senderForename;
    }

    public String getSenderSurname() {
        return senderSurname;
    }

    public String getBody() {
        return body;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isSeen() {
        return seen;
    }

    public Long getAttachmentFileId() {
        return attachmentFileId;
    }

    public String getAttachmentFileName() {
        return attachmentFileName;
    }

    public String getAttachmentContentType() {
        return attachmentContentType;
    }

    public long getAttachmentSizeBytes() {
        return attachmentSizeBytes == null ? 0L : attachmentSizeBytes;
    }
}
