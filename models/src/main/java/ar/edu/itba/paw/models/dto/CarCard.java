package ar.edu.itba.paw.models.dto;

import java.math.BigDecimal;

import ar.edu.itba.paw.models.domain.Listing;

/** Owner "my cars" hub card: always has car data, optionally has an active/paused listing. */
public final class CarCard {

    private final long carId;
    private final String brand;
    private final String model;
    private final long imageId;
    private final Long listingId;
    private final BigDecimal dayPrice;
    private final Listing.Status status;
    private final BigDecimal ratingAvg;

    public CarCard(
            final long carId,
            final String brand,
            final String model,
            final long imageId,
            final Long listingId,
            final BigDecimal dayPrice,
            final Listing.Status status,
            final BigDecimal ratingAvg) {
        this.carId = carId;
        this.brand = brand;
        this.model = model;
        this.imageId = imageId;
        this.listingId = listingId;
        this.dayPrice = dayPrice;
        this.status = status;
        this.ratingAvg = ratingAvg;
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

    public Long getListingId() {
        return listingId;
    }

    public BigDecimal getDayPrice() {
        return dayPrice;
    }

    public Listing.Status getStatus() {
        return status;
    }

    public BigDecimal getRatingAvg() {
        return ratingAvg;
    }

    public boolean isHasListing() {
        return listingId != null;
    }

    public String getStatusKey() {
        return status != null ? status.name() : null;
    }
}
