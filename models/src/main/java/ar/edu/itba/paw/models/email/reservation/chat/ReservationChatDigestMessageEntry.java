package ar.edu.itba.paw.models.email.reservation.chat;

import java.util.Objects;

/** One chat message line inside an hourly digest email. */
public final class ReservationChatDigestMessageEntry {

    private final String senderFullName;
    private final String messagePreview;
    private final String sentAt;

    public ReservationChatDigestMessageEntry(
            final String senderFullName, final String messagePreview, final String sentAt) {
        this.senderFullName = Objects.requireNonNull(senderFullName, "senderFullName");
        this.messagePreview = Objects.requireNonNull(messagePreview, "messagePreview");
        this.sentAt = Objects.requireNonNull(sentAt, "sentAt");
    }

    public String getSenderFullName() {
        return senderFullName;
    }

    public String getMessagePreview() {
        return messagePreview;
    }

    public String getSentAt() {
        return sentAt;
    }
}
