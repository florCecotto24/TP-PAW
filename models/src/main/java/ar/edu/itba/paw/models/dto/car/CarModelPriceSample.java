package ar.edu.itba.paw.models.dto.car;

import java.math.BigDecimal;

/**
 * One eligible active car's minimum offered {@code day_price} for a given catalog brand/model,
 * as returned by {@code CarDao#findActiveDayPricesForBrandModelPairs}. Used to batch-resolve
 * consumer market-price badges for a whole page of {@link CarCard}s in a single query: the caller
 * groups samples by brand/model and, per card, excludes the sample whose {@link #getCarId()}
 * matches the card itself before aggregating min/max/average.
 */
public final class CarModelPriceSample {

    private final String brand;
    private final String model;
    private final long carId;
    private final BigDecimal minPrice;

    public CarModelPriceSample(final String brand, final String model, final long carId, final BigDecimal minPrice) {
        this.brand = brand;
        this.model = model;
        this.carId = carId;
        this.minPrice = minPrice;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public long getCarId() {
        return carId;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }
}
