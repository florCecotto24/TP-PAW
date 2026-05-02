package ar.edu.itba.paw.models.email;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;

/**
 * Shared mail data for reservation lifecycle emails (confirmation, reminder, payment proof, listing deletion rider
 * parts, etc.). Rider-facing locale is {@link #getMessageLocale()}; owner-facing is {@link #getOwnerMailLocale()}.
 * Cancellation intro copy uses {@link ReservationCancellationEmailPayload}.
 */
public final class ReservationMailPayload {

    private final String recipientEmail;
    private final String riderFullName;
    private final long reservationId;
    private final long listingId;
    private final String vehicleLabel;
    private final OffsetDateTime startDate;
    private final OffsetDateTime endDate;
    /**
     * Pickup/return summary for the rider email. Before payment proof: public address (no street number).
     * After proof (second email): full address including number, see {@code ListingViewService#formatRiderReservationHandoverSummary}.
     */
    private final String riderHandoverLocation;
    /** Full data for the email to the owner. */
    private final String ownerHandoverLocation;
    private final String ownerFullName;
    private final String ownerEmail;
    private final String reservationTotal;
    private final Locale riderMailLocale;
    private final Locale ownerMailLocale;
    private final String ownerCbu;

    private ReservationMailPayload(final Builder builder) {
        this.recipientEmail = Objects.requireNonNull(builder.recipientEmail, "recipientEmail");
        this.riderFullName = Objects.requireNonNull(builder.riderFullName, "riderFullName");
        this.reservationId = Objects.requireNonNull(builder.reservationId, "reservationId");
        this.listingId = Objects.requireNonNull(builder.listingId, "listingId");
        this.vehicleLabel = Objects.requireNonNull(builder.vehicleLabel, "vehicleLabel");
        this.startDate = Objects.requireNonNull(builder.startDate, "startDate");
        this.endDate = Objects.requireNonNull(builder.endDate, "endDate");
        this.riderHandoverLocation = builder.riderHandoverLocation;
        this.ownerHandoverLocation = builder.ownerHandoverLocation;
        this.ownerFullName = builder.ownerFullName;
        this.ownerEmail = builder.ownerEmail;
        this.reservationTotal = builder.reservationTotal;
        this.riderMailLocale = Objects.requireNonNull(builder.riderMailLocale, "riderMailLocale");
        this.ownerMailLocale = Objects.requireNonNull(builder.ownerMailLocale, "ownerMailLocale");
        this.ownerCbu = builder.ownerCbu;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getRiderFullName() {
        return riderFullName;
    }

    public long getReservationId() {
        return reservationId;
    }

    public long getListingId() {
        return listingId;
    }

    public String getVehicleLabel() {
        return vehicleLabel;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public OffsetDateTime getEndDate() {
        return endDate;
    }

    public String getRiderHandoverLocation() {
        return riderHandoverLocation;
    }

    public String getOwnerHandoverLocation() {
        return ownerHandoverLocation;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public Locale getRiderMailLocale() {
        return riderMailLocale;
    }

    public Locale getOwnerMailLocale() {
        return ownerMailLocale;
    }

    public String getReservationTotal() {
        return reservationTotal;
    }

    public Locale getMessageLocale() {
        return riderMailLocale;
    }

    public String getDeliveryLocation() {
        return riderHandoverLocation;
    }

    public String getOwnerCbu() {
        return ownerCbu;
    }

    public static final class Builder {
        private String recipientEmail;
        private String riderFullName;
        private Long reservationId;
        private Long listingId;
        private String vehicleLabel;
        private OffsetDateTime startDate;
        private OffsetDateTime endDate;
        private String riderHandoverLocation;
        private String ownerHandoverLocation;
        private String ownerFullName;
        private String ownerEmail;
        private String reservationTotal;
        private Locale riderMailLocale;
        private Locale ownerMailLocale;
        private String ownerCbu;

        private Builder() {
        }

        public Builder recipientEmail(final String value) {
            this.recipientEmail = value;
            return this;
        }

        public Builder riderFullName(final String value) {
            this.riderFullName = value;
            return this;
        }

        public Builder reservationId(final long value) {
            this.reservationId = value;
            return this;
        }

        public Builder listingId(final long value) {
            this.listingId = value;
            return this;
        }

        public Builder vehicleLabel(final String value) {
            this.vehicleLabel = value;
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

        public Builder riderHandoverLocation(final String value) {
            this.riderHandoverLocation = value;
            return this;
        }

        public Builder ownerHandoverLocation(final String value) {
            this.ownerHandoverLocation = value;
            return this;
        }

        public Builder ownerFullName(final String value) {
            this.ownerFullName = value;
            return this;
        }

        public Builder ownerEmail(final String value) {
            this.ownerEmail = value;
            return this;
        }

        public Builder reservationTotal(final String value) {
            this.reservationTotal = value;
            return this;
        }

        public Builder riderMailLocale(final Locale value) {
            this.riderMailLocale = value;
            return this;
        }

        public Builder ownerMailLocale(final Locale value) {
            this.ownerMailLocale = value;
            return this;
        }

        public Builder ownerCbu(final String value) {
            this.ownerCbu = value;
            return this;
        }

        public ReservationMailPayload build() {
            return new ReservationMailPayload(this);
        }
    }
}
