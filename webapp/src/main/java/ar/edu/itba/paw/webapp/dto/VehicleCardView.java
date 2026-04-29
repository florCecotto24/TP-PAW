package ar.edu.itba.paw.webapp.dto;

import java.math.BigDecimal;

public final class VehicleCardView {
    private final long listingId;
    private final String brand;
    private final String model;
    private final BigDecimal price;
    private final long imageId;
    private final String statusKey;
    private final BigDecimal ratingAvg;

    public VehicleCardView(
            final long listingId,
            final String brand,
            final String model,
            final BigDecimal price,
            final long imageId) {
        this(listingId, brand, model, price, imageId, null, null);
    }

    public VehicleCardView(
            final long listingId,
            final String brand,
            final String model,
            final BigDecimal price,
            final long imageId,
            final String statusKey) {
        this(listingId, brand, model, price, imageId, statusKey, null);
    }

    public VehicleCardView(
            final long listingId,
            final String brand,
            final String model,
            final BigDecimal price,
            final long imageId,
            final String statusKey,
            final BigDecimal ratingAvg) {
        this.listingId = listingId;
        this.brand = brand;
        this.model = model;
        this.price = price;
        this.imageId = imageId;
        this.statusKey = statusKey;
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

    public String getStatusKey() {
        return statusKey;
    }

    public BigDecimal getRatingAvg() {
        return ratingAvg;
    }
}

