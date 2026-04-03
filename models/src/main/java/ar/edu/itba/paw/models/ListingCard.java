package ar.edu.itba.paw.models;

import java.math.BigDecimal;

public final class ListingCard {

    private final long listingId;
    private final String brand;
    private final String model;
    private final BigDecimal dayPrice;
    private final long imageId;

    public ListingCard(
            final long listingId,
            final String brand,
            final String model,
            final BigDecimal dayPrice,
            final long imageId) {
        this.listingId = listingId;
        this.brand = brand;
        this.model = model;
        this.dayPrice = dayPrice;
        this.imageId = imageId;
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

    public BigDecimal getDayPrice() {
        return dayPrice;
    }

    public long getImageId() {
        return imageId;
    }
}
