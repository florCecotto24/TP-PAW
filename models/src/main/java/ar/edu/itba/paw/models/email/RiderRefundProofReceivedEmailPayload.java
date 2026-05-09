package ar.edu.itba.paw.models.email;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;

/** Rider notified that the host uploaded a refund transfer receipt for a cancelled confirmed reservation. */
public final class RiderRefundProofReceivedEmailPayload {

    private final Locale messageLocale;
    private final String recipientEmail;
    private final String riderFullName;
    private final String ownerFullName;
    private final String ownerEmail;
    private final String reservationTotal;
    private final String vehicleLabel;
    private final long reservationId;
    private final OffsetDateTime startDate;
    private final OffsetDateTime endDate;

    private RiderRefundProofReceivedEmailPayload(final Builder b) {
        this.messageLocale = Objects.requireNonNull(b.messageLocale, "messageLocale");
        this.recipientEmail = Objects.requireNonNull(b.recipientEmail, "recipientEmail");
        this.riderFullName = Objects.requireNonNull(b.riderFullName, "riderFullName");
        this.ownerFullName = Objects.requireNonNull(b.ownerFullName, "ownerFullName");
        this.ownerEmail = b.ownerEmail != null ? b.ownerEmail : "";
        this.reservationTotal = Objects.requireNonNull(b.reservationTotal, "reservationTotal");
        this.vehicleLabel = Objects.requireNonNull(b.vehicleLabel, "vehicleLabel");
        this.reservationId = b.reservationId;
        this.startDate = Objects.requireNonNull(b.startDate, "startDate");
        this.endDate = Objects.requireNonNull(b.endDate, "endDate");
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

    public String getRiderFullName() {
        return riderFullName;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getReservationTotal() {
        return reservationTotal;
    }

    public String getVehicleLabel() {
        return vehicleLabel;
    }

    public long getReservationId() {
        return reservationId;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public OffsetDateTime getEndDate() {
        return endDate;
    }

    public static final class Builder {
        private Locale messageLocale;
        private String recipientEmail;
        private String riderFullName;
        private String ownerFullName;
        private String ownerEmail;
        private String reservationTotal;
        private String vehicleLabel;
        private long reservationId;
        private OffsetDateTime startDate;
        private OffsetDateTime endDate;

        public Builder messageLocale(final Locale v) {
            this.messageLocale = v;
            return this;
        }

        public Builder recipientEmail(final String v) {
            this.recipientEmail = v;
            return this;
        }

        public Builder riderFullName(final String v) {
            this.riderFullName = v;
            return this;
        }

        public Builder ownerFullName(final String v) {
            this.ownerFullName = v;
            return this;
        }

        public Builder ownerEmail(final String v) {
            this.ownerEmail = v;
            return this;
        }

        public Builder reservationTotal(final String v) {
            this.reservationTotal = v;
            return this;
        }

        public Builder vehicleLabel(final String v) {
            this.vehicleLabel = v;
            return this;
        }

        public Builder reservationId(final long v) {
            this.reservationId = v;
            return this;
        }

        public Builder startDate(final OffsetDateTime v) {
            this.startDate = v;
            return this;
        }

        public Builder endDate(final OffsetDateTime v) {
            this.endDate = v;
            return this;
        }

        public RiderRefundProofReceivedEmailPayload build() {
            return new RiderRefundProofReceivedEmailPayload(this);
        }
    }
}
