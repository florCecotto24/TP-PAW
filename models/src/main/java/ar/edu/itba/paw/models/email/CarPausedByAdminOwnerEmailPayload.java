package ar.edu.itba.paw.models.email;

import java.util.Locale;
import java.util.Objects;

/**
 * Notifies the car owner that a publication was paused by a platform administrator.
 * Use {@link #builder()} to construct instances.
 */
public final class CarPausedByAdminOwnerEmailPayload {

    private final Locale messageLocale;
    private final String ownerEmail;
    private final String ownerFullName;
    private final String vehicleLabel;
    private final long carId;

    private CarPausedByAdminOwnerEmailPayload(final Builder builder) {
        this.messageLocale = Objects.requireNonNull(builder.messageLocale, "messageLocale");
        this.ownerEmail = Objects.requireNonNull(builder.ownerEmail, "ownerEmail");
        this.ownerFullName = Objects.requireNonNull(builder.ownerFullName, "ownerFullName");
        this.vehicleLabel = Objects.requireNonNull(builder.vehicleLabel, "vehicleLabel");
        this.carId = builder.carId;
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

    public long getCarId() {
        return carId;
    }

    public static final class Builder {
        private Locale messageLocale;
        private String ownerEmail;
        private String ownerFullName;
        private String vehicleLabel;
        private long carId;

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

        public Builder carId(final long value) {
            this.carId = value;
            return this;
        }

        public CarPausedByAdminOwnerEmailPayload build() {
            return new CarPausedByAdminOwnerEmailPayload(this);
        }
    }
}
