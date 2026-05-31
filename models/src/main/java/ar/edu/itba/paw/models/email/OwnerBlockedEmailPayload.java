package ar.edu.itba.paw.models.email;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Notifies an owner that their account has been blocked because one or more refund-proof deadlines lapsed.
 * Includes the list of {@link OverdueRefundReservation overdue reservations} so the owner knows exactly which
 * proofs are pending.
 */
public final class OwnerBlockedEmailPayload {

    private final Locale messageLocale;
    private final String recipientEmail;
    private final String ownerFullName;
    private final List<OverdueRefundReservation> overdueReservations;

    private OwnerBlockedEmailPayload(final Builder b) {
        this.messageLocale = Objects.requireNonNull(b.messageLocale, "messageLocale");
        this.recipientEmail = Objects.requireNonNull(b.recipientEmail, "recipientEmail");
        this.ownerFullName = Objects.requireNonNull(b.ownerFullName, "ownerFullName");
        Objects.requireNonNull(b.overdueReservations, "overdueReservations");
        if (b.overdueReservations.isEmpty()) {
            throw new IllegalArgumentException("overdueReservations must not be empty");
        }
        this.overdueReservations = Collections.unmodifiableList(new ArrayList<>(b.overdueReservations));
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

    public List<OverdueRefundReservation> getOverdueReservations() {
        return overdueReservations;
    }

    public static final class Builder {
        private Locale messageLocale;
        private String recipientEmail;
        private String ownerFullName;
        private List<OverdueRefundReservation> overdueReservations;

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

        public Builder overdueReservations(final List<OverdueRefundReservation> v) {
            this.overdueReservations = v;
            return this;
        }

        public OwnerBlockedEmailPayload build() {
            return new OwnerBlockedEmailPayload(this);
        }
    }

    /** Immutable row describing a single reservation with a lapsed refund-proof deadline. */
    public static final class OverdueRefundReservation {
        private final long reservationId;
        private final String vehicleLabel;
        private final OffsetDateTime refundProofDeadlineAt;
        private final String reservationTotal;

        public OverdueRefundReservation(
                final long reservationId,
                final String vehicleLabel,
                final OffsetDateTime refundProofDeadlineAt,
                final String reservationTotal) {
            this.reservationId = reservationId;
            this.vehicleLabel = Objects.requireNonNull(vehicleLabel, "vehicleLabel");
            this.refundProofDeadlineAt = Objects.requireNonNull(refundProofDeadlineAt, "refundProofDeadlineAt");
            this.reservationTotal = Objects.requireNonNull(reservationTotal, "reservationTotal");
        }

        public long getReservationId() {
            return reservationId;
        }

        public String getVehicleLabel() {
            return vehicleLabel;
        }

        public OffsetDateTime getRefundProofDeadlineAt() {
            return refundProofDeadlineAt;
        }

        public String getReservationTotal() {
            return reservationTotal;
        }
    }
}
