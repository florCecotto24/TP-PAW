package ar.edu.itba.paw.models.email;

import java.util.List;
import java.util.Objects;

/** Reservation-scoped group of chat messages in an hourly digest email. */
public final class ReservationChatDigestConversationEntry {

    private final String vehicleLabel;
    private final long reservationId;
    private final String detailUrl;
    private final List<ReservationChatDigestMessageEntry> messages;

    public ReservationChatDigestConversationEntry(
            final String vehicleLabel,
            final long reservationId,
            final String detailUrl,
            final List<ReservationChatDigestMessageEntry> messages) {
        this.vehicleLabel = Objects.requireNonNull(vehicleLabel, "vehicleLabel");
        this.reservationId = reservationId;
        this.detailUrl = Objects.requireNonNull(detailUrl, "detailUrl");
        this.messages = List.copyOf(Objects.requireNonNull(messages, "messages"));
    }

    public String getVehicleLabel() {
        return vehicleLabel;
    }

    public long getReservationId() {
        return reservationId;
    }

    public String getDetailUrl() {
        return detailUrl;
    }

    public List<ReservationChatDigestMessageEntry> getMessages() {
        return messages;
    }
}
