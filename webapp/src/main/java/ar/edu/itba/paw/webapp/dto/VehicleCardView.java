package ar.edu.itba.paw.webapp.dto;

import java.math.BigDecimal;

public class VehicleCardView {
    private final long listingId;
    private final String brand;
    private final String model;
    private final BigDecimal price;
    private final long imageId;

    public VehicleCardView(
            final long listingId,
            final String brand,
            final String model,
            final BigDecimal price,
            final long imageId) {
        this.listingId = listingId;
        this.brand = brand;
        this.model = model;
        this.price = price;
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

    public BigDecimal getPrice() {
        return price;
    }

    public long getImageId() {
        return imageId;
    }
}

