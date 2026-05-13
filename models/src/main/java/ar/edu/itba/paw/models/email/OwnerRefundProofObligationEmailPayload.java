package ar.edu.itba.paw.models.email;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;

/**
 * Owner must upload a refund transfer proof after a confirmed reservation was cancelled; also used for deadline
 * reminders ({@link #isDueReminder()}).
 */
public final class OwnerRefundProofObligationEmailPayload {

    private final Locale messageLocale;
    private final String recipientEmail;
    private final String ownerFullName;
    private final String vehicleLabel;
    private final long reservationId;
    private final OffsetDateTime startDate;
    private final OffsetDateTime endDate;
    private final String reservationTotal;
    private final OffsetDateTime refundProofDeadlineAt;
    private final boolean dueReminder;

    private OwnerRefundProofObligationEmailPayload(final Builder b) {
        this.messageLocale = Objects.requireNonNull(b.messageLocale, "messageLocale");
        this.recipientEmail = Objects.requireNonNull(b.recipientEmail, "recipientEmail");
        this.ownerFullName = Objects.requireNonNull(b.ownerFullName, "ownerFullName");
        this.vehicleLabel = Objects.requireNonNull(b.vehicleLabel, "vehicleLabel");
        this.reservationId = b.reservationId;
        this.startDate = Objects.requireNonNull(b.startDate, "startDate");
        this.endDate = Objects.requireNonNull(b.endDate, "endDate");
        this.reservationTotal = Objects.requireNonNull(b.reservationTotal, "reservationTotal");
        this.refundProofDeadlineAt = Objects.requireNonNull(b.refundProofDeadlineAt, "refundProofDeadlineAt");
        this.dueReminder = b.dueReminder;
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

    public String getReservationTotal() {
        return reservationTotal;
    }

    public OffsetDateTime getRefundProofDeadlineAt() {
        return refundProofDeadlineAt;
    }

    public boolean isDueReminder() {
        return dueReminder;
    }

    public static final class Builder {
        private Locale messageLocale;
        private String recipientEmail;
        private String ownerFullName;
        private String vehicleLabel;
        private long reservationId;
        private OffsetDateTime startDate;
        private OffsetDateTime endDate;
        private String reservationTotal;
        private OffsetDateTime refundProofDeadlineAt;
        private boolean dueReminder;

        public Builder messageLocale(final Locale v) {
            this.messageLocale = v;
            return this;
        }

        public Builder recipientEmail(final String v) {
            this.recipientEmail = v;
            return this;
        }

        public Builder ownerFullName(final String v) {
            this.ownerFullName = v;
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

        public Builder reservationTotal(final String v) {
            this.reservationTotal = v;
            return this;
        }

        public Builder refundProofDeadlineAt(final OffsetDateTime v) {
            this.refundProofDeadlineAt = v;
            return this;
        }

        public Builder dueReminder(final boolean v) {
            this.dueReminder = v;
            return this;
        }

        public OwnerRefundProofObligationEmailPayload build() {
            return new OwnerRefundProofObligationEmailPayload(this);
        }
    }
}
