package ar.edu.itba.paw.models.email;

import java.util.Locale;
import java.util.Objects;

/**
 * Notifies the car owner that an admin rejected the catalog entry (brand, model, or both) used by their car.
 * Use {@link #builder()} to construct instances.
 */
public final class CarRejectedByAdminOwnerEmailPayload {

    private final Locale messageLocale;
    private final String ownerEmail;
    private final String ownerFullName;
    private final String vehicleLabel;
    private final long carId;
    private final String brandName;
    private final String modelName;
    private final boolean brandRejected;
    private final boolean modelRejected;

    private CarRejectedByAdminOwnerEmailPayload(final Builder builder) {
        this.messageLocale = Objects.requireNonNull(builder.messageLocale, "messageLocale");
        this.ownerEmail = Objects.requireNonNull(builder.ownerEmail, "ownerEmail");
        this.ownerFullName = Objects.requireNonNull(builder.ownerFullName, "ownerFullName");
        this.vehicleLabel = Objects.requireNonNull(builder.vehicleLabel, "vehicleLabel");
        this.carId = builder.carId;
        this.brandName = builder.brandName;
        this.modelName = builder.modelName;
        this.brandRejected = builder.brandRejected;
        this.modelRejected = builder.modelRejected;
        if (!this.brandRejected && !this.modelRejected) {
            throw new IllegalArgumentException("At least one of brandRejected or modelRejected must be true");
        }
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

    public String getBrandName() {
        return brandName;
    }

    public String getModelName() {
        return modelName;
    }

    public boolean isBrandRejected() {
        return brandRejected;
    }

    public boolean isModelRejected() {
        return modelRejected;
    }

    public static final class Builder {
        private Locale messageLocale;
        private String ownerEmail;
        private String ownerFullName;
        private String vehicleLabel;
        private long carId;
        private String brandName;
        private String modelName;
        private boolean brandRejected;
        private boolean modelRejected;

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

        public Builder brandName(final String value) {
            this.brandName = value;
            return this;
        }

        public Builder modelName(final String value) {
            this.modelName = value;
            return this;
        }

        public Builder brandRejected(final boolean value) {
            this.brandRejected = value;
            return this;
        }

        public Builder modelRejected(final boolean value) {
            this.modelRejected = value;
            return this;
        }

        public CarRejectedByAdminOwnerEmailPayload build() {
            return new CarRejectedByAdminOwnerEmailPayload(this);
        }
    }
}
