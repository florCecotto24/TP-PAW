package ar.edu.itba.paw.models.dto.profile;

import java.math.BigDecimal;
import java.util.Objects;

/** Active listing row for the counterparty profile grid ({@code carCard} / JSP). */
public final class CounterpartyActiveListingCardRow {

    private final long listingId;
    private final String brand;
    private final String model;
    private final BigDecimal price;
    private final long imageId;
    private final BigDecimal ratingAvg;

    public CounterpartyActiveListingCardRow(
            final long listingId,
            final String brand,
            final String model,
            final BigDecimal price,
            final long imageId,
            final BigDecimal ratingAvg) {
        this.listingId = listingId;
        this.brand = Objects.requireNonNull(brand, "brand");
        this.model = Objects.requireNonNull(model, "model");
        this.price = Objects.requireNonNull(price, "price");
        this.imageId = imageId;
        this.ratingAvg = ratingAvg;
    }

    public long getListingId() {
        return listingId;
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
}
