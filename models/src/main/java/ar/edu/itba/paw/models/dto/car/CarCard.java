package ar.edu.itba.paw.models.dto.car;

import java.math.BigDecimal;

import ar.edu.itba.paw.models.domain.Car;

/**
 * Car teaser projected from {@code cars} and the latest listing data: identity, cover image,
 * lowest day price (when published), status, average rating and (optionally) the owner id used
 * by views to hide the "favorite" heart on own cars.
 */
public final class CarCard {

    private final long carId;
    private final String brand;
    private final String model;
    private final long imageId;
    private final BigDecimal dayPrice;
    private final Car.Status status;
    private final BigDecimal ratingAvg;
    /** True when the car's catalog model was created on the fly and has not yet been validated by an admin. */
    private final boolean modelPendingValidation;
    private final int minimumRentalDays;
    /** Optional: the user that owns the car. Used by views to hide the favorite heart on own cars. */
    private final Long ownerId;

    private CarCard(final Builder b) {
        this.carId = b.carId;
        this.brand = b.brand;
        this.model = b.model;
        this.imageId = b.imageId;
        this.dayPrice = b.dayPrice;
        this.status = b.status;
        this.ratingAvg = b.ratingAvg;
        this.modelPendingValidation = b.modelPendingValidation;
        this.minimumRentalDays = b.minimumRentalDays;
        this.ownerId = b.ownerId;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CarCard} (Effective Java, Item 2: avoids the telescoping-constructor smell
     * for the many optional fields — cover image, day price, status, rating, owner id, minimum
     * rental days). Required fields ({@code carId}, {@code brand}, {@code model}) are validated in
     * {@link #build()}; everything else has a sensible default ({@code imageId=0L}, {@code
     * minimumRentalDays=1}, nulls or {@code false}).
     */
    public static final class Builder {
        private long carId;
        private String brand;
        private String model;
        /** {@code 0L} means "no cover image". */
        private long imageId;
        private BigDecimal dayPrice;
        private Car.Status status;
        private BigDecimal ratingAvg;
        private boolean modelPendingValidation;
        private int minimumRentalDays = 1;
        private Long ownerId;

        public Builder carId(final long carId) {
            this.carId = carId;
            return this;
        }

        public Builder brand(final String brand) {
            this.brand = brand;
            return this;
        }

        public Builder model(final String model) {
            this.model = model;
            return this;
        }

        public Builder imageId(final long imageId) {
            this.imageId = imageId;
            return this;
        }

        public Builder dayPrice(final BigDecimal dayPrice) {
            this.dayPrice = dayPrice;
            return this;
        }

        public Builder status(final Car.Status status) {
            this.status = status;
            return this;
        }

        public Builder ratingAvg(final BigDecimal ratingAvg) {
            this.ratingAvg = ratingAvg;
            return this;
        }

        public Builder modelPendingValidation(final boolean modelPendingValidation) {
            this.modelPendingValidation = modelPendingValidation;
            return this;
        }

        public Builder minimumRentalDays(final int minimumRentalDays) {
            this.minimumRentalDays = minimumRentalDays;
            return this;
        }

        public Builder ownerId(final Long ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public CarCard build() {
            if (brand == null) {
                throw new IllegalStateException("brand is required");
            }
            if (model == null) {
                throw new IllegalStateException("model is required");
            }
            return new CarCard(this);
        }
    }

    public long getCarId() {
        return carId;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public long getImageId() {
        return imageId;
    }

    public BigDecimal getDayPrice() {
        return dayPrice;
    }

    public Car.Status getStatus() {
        return status;
    }

    public BigDecimal getRatingAvg() {
        return ratingAvg;
    }

    /** True when the car has a current day price (i.e. at least one offered availability row). */
    public boolean isHasListing() {
        return dayPrice != null;
    }

    public String getStatusKey() {
        return status != null ? status.name() : null;
    }

    /** True when the car's catalog model was created on the fly and has not yet been validated by an admin. */
    public boolean isModelPendingValidation() {
        return modelPendingValidation;
    }

    public int getMinimumRentalDays() {
        return minimumRentalDays;
    }

    /** Owner user id when known; {@code null} when the producing DAO didn't surface it. */
    public Long getOwnerId() {
        return ownerId;
    }
}
