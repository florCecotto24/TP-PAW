package ar.edu.itba.paw.models.email;

import java.util.Locale;
import java.util.Objects;

/**
 * Notifies the listing owner that a publication was paused because their profile has no valid CBU.
 * Use {@link #builder()} to construct instances.
 */
public final class ListingPausedMissingCbuOwnerEmailPayload {

    private final Locale messageLocale;
    private final String ownerEmail;
    private final String ownerFullName;
    private final String vehicleLabel;
    private final long listingId;

    private ListingPausedMissingCbuOwnerEmailPayload(final Builder builder) {
        this.messageLocale = Objects.requireNonNull(builder.messageLocale, "messageLocale");
        this.ownerEmail = Objects.requireNonNull(builder.ownerEmail, "ownerEmail");
        this.ownerFullName = Objects.requireNonNull(builder.ownerFullName, "ownerFullName");
        this.vehicleLabel = Objects.requireNonNull(builder.vehicleLabel, "vehicleLabel");
        this.listingId = builder.listingId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Locale getMessageLocale() {
        return messageLocale;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public String getVehicleLabel() {
        return vehicleLabel;
    }

    public long getListingId() {
        return listingId;
    }

    public static final class Builder {
        private Locale messageLocale;
        private String ownerEmail;
        private String ownerFullName;
        private String vehicleLabel;
        private long listingId;

        private Builder() {
        }

        public Builder messageLocale(final Locale value) {
            this.messageLocale = value;
            return this;
        }

        public Builder ownerEmail(final String value) {
            this.ownerEmail = value;
            return this;
        }

        public Builder ownerFullName(final String value) {
            this.ownerFullName = value;
            return this;
        }

        public Builder vehicleLabel(final String value) {
            this.vehicleLabel = value;
            return this;
        }

        public Builder listingId(final long value) {
            this.listingId = value;
            return this;
        }

        public ListingPausedMissingCbuOwnerEmailPayload build() {
            return new ListingPausedMissingCbuOwnerEmailPayload(this);
        }
    }
}
