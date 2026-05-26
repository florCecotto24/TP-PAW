package ar.edu.itba.paw.models.dto;

import java.math.BigDecimal;

import ar.edu.itba.paw.models.domain.Car;

/** Owner "my cars" hub card: car identity plus the latest available day price (when published). */
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

    public CarCard(
            final long carId,
            final String brand,
            final String model,
            final long imageId,
            final BigDecimal dayPrice,
            final Car.Status status,
            final BigDecimal ratingAvg) {
        this(carId, brand, model, imageId, dayPrice, status, ratingAvg, false);
    }

    public CarCard(
            final long carId,
            final String brand,
            final String model,
            final long imageId,
            final BigDecimal dayPrice,
            final Car.Status status,
            final BigDecimal ratingAvg,
            final boolean modelPendingValidation) {
        this.carId = carId;
        this.brand = brand;
        this.model = model;
        this.imageId = imageId;
        this.dayPrice = dayPrice;
        this.status = status;
        this.ratingAvg = ratingAvg;
        this.modelPendingValidation = modelPendingValidation;
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
}
