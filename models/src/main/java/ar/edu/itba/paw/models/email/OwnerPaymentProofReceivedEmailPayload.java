package ar.edu.itba.paw.models.email;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;

/**
 * Notifies the listing owner that the rider uploaded a payment receipt and can view it.
 * Use {@link #builder()} to construct instances.
 */
public final class OwnerPaymentProofReceivedEmailPayload {

    private final Locale messageLocale;
    private final String recipientEmail;
    private final String ownerFullName;
    private final String riderFullName;
    private final String riderEmail;
    private final String reservationTotal;
    private final String vehicleLabel;
    private final long reservationId;
    private final OffsetDateTime startDate;
    private final OffsetDateTime endDate;

    private OwnerPaymentProofReceivedEmailPayload(final Builder builder) {
        this.messageLocale = Objects.requireNonNull(builder.messageLocale, "messageLocale");
        this.recipientEmail = Objects.requireNonNull(builder.recipientEmail, "recipientEmail");
        this.ownerFullName = Objects.requireNonNull(builder.ownerFullName, "ownerFullName");
        this.riderFullName = Objects.requireNonNull(builder.riderFullName, "riderFullName");
        this.riderEmail = builder.riderEmail != null ? builder.riderEmail : "";
        this.reservationTotal = Objects.requireNonNull(builder.reservationTotal, "reservationTotal");
        this.vehicleLabel = Objects.requireNonNull(builder.vehicleLabel, "vehicleLabel");
        this.reservationId = Objects.requireNonNull(builder.reservationId, "reservationId");
        this.startDate = Objects.requireNonNull(builder.startDate, "startDate");
        this.endDate = Objects.requireNonNull(builder.endDate, "endDate");
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

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public String getRiderFullName() {
        return riderFullName;
    }

    public String getRiderEmail() {
        return riderEmail;
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
        private String ownerFullName;
        private String riderFullName;
        private String riderEmail;
        private String reservationTotal;
        private String vehicleLabel;
        private Long reservationId;
        private OffsetDateTime startDate;
        private OffsetDateTime endDate;

        private Builder() {
        }

        public Builder messageLocale(final Locale value) {
            this.messageLocale = value;
            return this;
        }

        public Builder recipientEmail(final String value) {
            this.recipientEmail = value;
            return this;
        }

        public Builder ownerFullName(final String value) {
            this.ownerFullName = value;
            return this;
        }

        public Builder riderFullName(final String value) {
            this.riderFullName = value;
            return this;
        }

        public Builder riderEmail(final String value) {
            this.riderEmail = value;
            return this;
        }

        public Builder reservationTotal(final String value) {
            this.reservationTotal = value;
            return this;
        }

        public Builder vehicleLabel(final String value) {
            this.vehicleLabel = value;
            return this;
        }

        public Builder reservationId(final long value) {
            this.reservationId = value;
            return this;
        }

        public Builder startDate(final OffsetDateTime value) {
            this.startDate = value;
            return this;
        }

        public Builder endDate(final OffsetDateTime value) {
            this.endDate = value;
            return this;
        }

        public OwnerPaymentProofReceivedEmailPayload build() {
            return new OwnerPaymentProofReceivedEmailPayload(this);
        }
    }
}
