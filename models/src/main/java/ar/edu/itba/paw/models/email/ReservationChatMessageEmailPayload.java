package ar.edu.itba.paw.models.email;

import java.util.Locale;
import java.util.Objects;

/** Notifies a reservation participant that the counterparty sent a chat message. */
public final class ReservationChatMessageEmailPayload {

    private final Locale messageLocale;
    private final String recipientEmail;
    private final String recipientFullName;
    private final String senderFullName;
    private final String messagePreview;
    private final String vehicleLabel;
    private final long reservationId;
    private final String detailUrl;

    private ReservationChatMessageEmailPayload(final Builder builder) {
        this.messageLocale = Objects.requireNonNull(builder.messageLocale, "messageLocale");
        this.recipientEmail = Objects.requireNonNull(builder.recipientEmail, "recipientEmail");
        this.recipientFullName = Objects.requireNonNull(builder.recipientFullName, "recipientFullName");
        this.senderFullName = Objects.requireNonNull(builder.senderFullName, "senderFullName");
        this.messagePreview = Objects.requireNonNull(builder.messagePreview, "messagePreview");
        this.vehicleLabel = Objects.requireNonNull(builder.vehicleLabel, "vehicleLabel");
        this.reservationId = Objects.requireNonNull(builder.reservationId, "reservationId");
        this.detailUrl = Objects.requireNonNull(builder.detailUrl, "detailUrl");
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

    public String getSenderFullName() {
        return senderFullName;
    }

    public String getMessagePreview() {
        return messagePreview;
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

    public static final class Builder {
        private Locale messageLocale;
        private String recipientEmail;
        private String recipientFullName;
        private String senderFullName;
        private String messagePreview;
        private String vehicleLabel;
        private Long reservationId;
        private String detailUrl;

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

        public Builder senderFullName(final String senderFullName) {
            this.senderFullName = senderFullName;
            return this;
        }

        public Builder messagePreview(final String messagePreview) {
            this.messagePreview = messagePreview;
            return this;
        }

        public Builder vehicleLabel(final String vehicleLabel) {
            this.vehicleLabel = vehicleLabel;
            return this;
        }

        public Builder reservationId(final long reservationId) {
            this.reservationId = reservationId;
            return this;
        }

        public Builder detailUrl(final String detailUrl) {
            this.detailUrl = detailUrl;
            return this;
        }

        public ReservationChatMessageEmailPayload build() {
            return new ReservationChatMessageEmailPayload(this);
        }
    }
}
