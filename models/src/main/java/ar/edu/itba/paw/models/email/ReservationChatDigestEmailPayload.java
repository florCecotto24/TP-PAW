package ar.edu.itba.paw.models.email;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Hourly digest of reservation chat messages for one recipient. */
public final class ReservationChatDigestEmailPayload {

    private final Locale messageLocale;
    private final String recipientEmail;
    private final String recipientFullName;
    private final int totalMessageCount;
    private final List<ReservationChatDigestConversationEntry> conversations;

    private ReservationChatDigestEmailPayload(final Builder builder) {
        this.messageLocale = Objects.requireNonNull(builder.messageLocale, "messageLocale");
        this.recipientEmail = Objects.requireNonNull(builder.recipientEmail, "recipientEmail");
        this.recipientFullName = Objects.requireNonNull(builder.recipientFullName, "recipientFullName");
        this.totalMessageCount = builder.totalMessageCount;
        this.conversations = List.copyOf(Objects.requireNonNull(builder.conversations, "conversations"));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Locale getMessageLocale() {
        return messageLocale;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getRecipientFullName() {
        return recipientFullName;
    }

    public int getTotalMessageCount() {
        return totalMessageCount;
    }

    public List<ReservationChatDigestConversationEntry> getConversations() {
        return conversations;
    }

    public static final class Builder {
        private Locale messageLocale;
        private String recipientEmail;
        private String recipientFullName;
        private int totalMessageCount;
        private List<ReservationChatDigestConversationEntry> conversations;

        public Builder messageLocale(final Locale messageLocale) {
            this.messageLocale = messageLocale;
            return this;
        }

        public Builder recipientEmail(final String recipientEmail) {
            this.recipientEmail = recipientEmail;
            return this;
        }

        public Builder recipientFullName(final String recipientFullName) {
            this.recipientFullName = recipientFullName;
            return this;
        }

        public Builder totalMessageCount(final int totalMessageCount) {
            this.totalMessageCount = totalMessageCount;
            return this;
        }

        public Builder conversations(final List<ReservationChatDigestConversationEntry> conversations) {
            this.conversations = conversations;
            return this;
        }

        public ReservationChatDigestEmailPayload build() {
            return new ReservationChatDigestEmailPayload(this);
        }
    }
}
