package ar.edu.itba.paw.models.email;

import java.util.Locale;
import java.util.Objects;

/**
 * Data for emails asking the rider to return the vehicle (reminder before checkout or at checkout).
 * Use {@link #builder()} to construct instances.
 */
public final class RiderCarReturnEmailPayload {

    private final Locale messageLocale;
    private final String recipientEmail;
    private final String riderFullName;
    private final String vehicleLabel;
    private final String ownerEmail;
    private final String checkoutFormatted;
    private final String returnLocationLine;
    private final String reservationDetailPath;

    private RiderCarReturnEmailPayload(final Builder builder) {
        this.messageLocale = Objects.requireNonNull(builder.messageLocale, "messageLocale");
        this.recipientEmail = Objects.requireNonNull(builder.recipientEmail, "recipientEmail");
        this.riderFullName = Objects.requireNonNull(builder.riderFullName, "riderFullName");
        this.vehicleLabel = Objects.requireNonNull(builder.vehicleLabel, "vehicleLabel");
        this.ownerEmail = builder.ownerEmail == null ? "" : builder.ownerEmail;
        this.checkoutFormatted = builder.checkoutFormatted == null ? "" : builder.checkoutFormatted;
        this.returnLocationLine = builder.returnLocationLine == null ? "" : builder.returnLocationLine;
        this.reservationDetailPath = Objects.requireNonNull(builder.reservationDetailPath, "reservationDetailPath");
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

    public String getVehicleLabel() {
        return vehicleLabel;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getCheckoutFormatted() {
        return checkoutFormatted;
    }

    public String getReturnLocationLine() {
        return returnLocationLine;
    }

    /** Context-relative path, e.g. {@code /my-reservations/12}. */
    public String getReservationDetailPath() {
        return reservationDetailPath;
    }

    public static final class Builder {
        private Locale messageLocale;
        private String recipientEmail;
        private String riderFullName;
        private String vehicleLabel;
        private String ownerEmail;
        private String checkoutFormatted;
        private String returnLocationLine;
        private String reservationDetailPath;

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

        public Builder riderFullName(final String value) {
            this.riderFullName = value;
            return this;
        }

        public Builder vehicleLabel(final String value) {
            this.vehicleLabel = value;
            return this;
        }

        public Builder ownerEmail(final String value) {
            this.ownerEmail = value;
            return this;
        }

        public Builder checkoutFormatted(final String value) {
            this.checkoutFormatted = value;
            return this;
        }

        public Builder returnLocationLine(final String value) {
            this.returnLocationLine = value;
            return this;
        }

        public Builder reservationDetailPath(final String value) {
            this.reservationDetailPath = value;
            return this;
        }

        public RiderCarReturnEmailPayload build() {
            return new RiderCarReturnEmailPayload(this);
        }
    }
}
