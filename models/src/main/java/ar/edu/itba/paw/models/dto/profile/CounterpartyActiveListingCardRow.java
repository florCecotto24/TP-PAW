package ar.edu.itba.paw.models.dto.profile;

import java.math.BigDecimal;
import java.util.Objects;

/** Active listing row for the counterparty profile grid ({@code carCard} / JSP). */
public final class CounterpartyActiveListingCardRow {

    private final long carId;
    private final String brand;
    private final String model;
    private final BigDecimal price;
    private final long imageId;
    private final BigDecimal ratingAvg;
    private final long reviewCount;

    public CounterpartyActiveListingCardRow(
            final long carId,
            final String brand,
            final String model,
            final BigDecimal price,
            final long imageId,
            final BigDecimal ratingAvg,
            final long reviewCount) {
        this.carId = carId;
        this.brand = Objects.requireNonNull(brand, "brand");
        this.model = Objects.requireNonNull(model, "model");
        this.price = Objects.requireNonNull(price, "price");
        this.imageId = imageId;
        this.ratingAvg = ratingAvg;
        this.reviewCount = reviewCount;
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

    public BigDecimal getPrice() {
        return price;
    }

    public long getImageId() {
        return imageId;
    }

    public BigDecimal getRatingAvg() {
        return ratingAvg;
    }

    public long getReviewCount() {
        return reviewCount;
    }
}
