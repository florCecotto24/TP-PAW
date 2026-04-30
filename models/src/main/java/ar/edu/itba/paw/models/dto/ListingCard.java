package ar.edu.itba.paw.models.dto;

import java.math.BigDecimal;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.Listing;

public final class ListingCard {

    private final long listingId;
    private final String brand;
    private final String model;
    private final BigDecimal dayPrice;
    private final long imageId;
    private final BigDecimal ratingAvg;
    private final Listing.Status status;

    public ListingCard(
            final long listingId,
            final String brand,
            final String model,
            final BigDecimal dayPrice,
            final long imageId) {
        this(listingId, brand, model, dayPrice, imageId, null, null);
    }

    public ListingCard(
            final long listingId,
            final String brand,
            final String model,
            final BigDecimal dayPrice,
            final long imageId,
            final BigDecimal ratingAvg) {
        this(listingId, brand, model, dayPrice, imageId, ratingAvg, null);
    }

    public ListingCard(
            final long listingId,
            final String brand,
            final String model,
            final BigDecimal dayPrice,
            final long imageId,
            final BigDecimal ratingAvg,
            final Listing.Status status) {
        this.listingId = listingId;
        this.brand = brand;
        this.model = model;
        this.dayPrice = dayPrice;
        this.imageId = imageId;
        this.ratingAvg = ratingAvg;
        this.status = status;
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

    public Optional<BigDecimal> getRatingAvg() {
        return Optional.ofNullable(ratingAvg);
    }

    public Optional<Listing.Status> getStatus() {
        return Optional.ofNullable(status);
    }
}
