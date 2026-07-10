package ar.edu.itba.paw.models.dto.reservation;

import java.time.OffsetDateTime;
import java.util.Objects;

/** JSON payload for a reservation chat message. */
public final class ReservationMessageDto {

    private final long id;
    private final long reservationId;
    private final long senderUserId;
    private final String senderDisplayName;
    private final String body;
    private final OffsetDateTime createdAt;
    private final ReservationMessageAttachmentDto attachment;
    private final boolean seen;

    public ReservationMessageDto(
            final long id,
            final long reservationId,
            final long senderUserId,
            final String senderDisplayName,
            final String body,
            final OffsetDateTime createdAt,
            final ReservationMessageAttachmentDto attachment,
            final boolean seen) {
        this.id = id;
        this.reservationId = reservationId;
        this.senderUserId = senderUserId;
        this.senderDisplayName = Objects.requireNonNull(senderDisplayName, "senderDisplayName");
        this.body = body == null ? "" : body;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.attachment = attachment;
        this.seen = seen;
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

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public String getBody() {
        return body;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public ReservationMessageAttachmentDto getAttachment() {
        return attachment;
    }

    public boolean isSeen() {
        return seen;
    }
}
