package ar.edu.itba.paw.models.dto;

import java.time.OffsetDateTime;
import java.util.Objects;

/** JSON / STOMP payload for a reservation chat message. */
public final class ReservationMessageDto {

    private final long id;
    private final long reservationId;
    private final long senderUserId;
    private final String senderDisplayName;
    private final String body;
    private final OffsetDateTime createdAt;

    public ReservationMessageDto(
            final long id,
            final long reservationId,
            final long senderUserId,
            final String senderDisplayName,
            final String body,
            final OffsetDateTime createdAt) {
        this.id = id;
        this.reservationId = reservationId;
        this.senderUserId = senderUserId;
        this.senderDisplayName = Objects.requireNonNull(senderDisplayName, "senderDisplayName");
        this.body = Objects.requireNonNull(body, "body");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
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
}
