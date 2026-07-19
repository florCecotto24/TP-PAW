package ar.edu.itba.paw.models.dto.reservation;

import java.time.OffsetDateTime;
import java.util.Objects;

/** JSON payload for a reservation chat message. */
public final class ReservationMessageDto {

    private final long id;
    private final long reservationId;
    private final long senderUserId;
    private final String senderForename;
    private final String senderSurname;
    private final String body;
    private final OffsetDateTime createdAt;
    private final ReservationMessageAttachmentDto attachment;
    private final boolean seen;

    public ReservationMessageDto(
            final long id,
            final long reservationId,
            final long senderUserId,
            final String senderForename,
            final String senderSurname,
            final String body,
            final OffsetDateTime createdAt,
            final ReservationMessageAttachmentDto attachment,
            final boolean seen) {
        this.id = id;
        this.reservationId = reservationId;
        this.senderUserId = senderUserId;
        this.senderForename = Objects.requireNonNull(senderForename, "senderForename");
        this.senderSurname = Objects.requireNonNull(senderSurname, "senderSurname");
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

    public String getSenderForename() {
        return senderForename;
    }

    public String getSenderSurname() {
        return senderSurname;
    }

    /** Derived full name for digests and email previews. */
    public String getSenderDisplayName() {
        return senderForename + " " + senderSurname;
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
