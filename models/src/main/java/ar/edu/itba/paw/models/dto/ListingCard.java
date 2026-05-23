package ar.edu.itba.paw.models.dto;

import java.math.BigDecimal;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.Listing;

/** Compact listing row for search, home sections, and reservation cards (price, cover image, optional rating and status). */
public final class ListingCard {

    private final long listingId;
    private final long carId;
    private final String brand;
    private final String model;
    private final BigDecimal dayPrice;
    private final long imageId;
    private final BigDecimal ratingAvg;
    private final Listing.Status status;
    private final long reviewCount;

    public ListingCard(
            final long listingId,
            final long carId,
            final String brand,
            final String model,
            final BigDecimal dayPrice,
            final long imageId) {
        this(listingId, carId, brand, model, dayPrice, imageId, null, null, 0);
    }

    public ListingCard(
            final long listingId,
            final long carId,
            final String brand,
            final String model,
            final BigDecimal dayPrice,
            final long imageId,
            final BigDecimal ratingAvg) {
        this(listingId, carId, brand, model, dayPrice, imageId, ratingAvg, null, 0);
    }

    public ListingCard(
            final long listingId,
            final long carId,
            final String brand,
            final String model,
            final BigDecimal dayPrice,
            final long imageId,
            final BigDecimal ratingAvg,
            final Listing.Status status) {
        this(listingId, carId, brand, model, dayPrice, imageId, ratingAvg, status, 0);
    }

    public ListingCard(
            final long listingId,
            final long carId,
            final String brand,
            final String model,
            final BigDecimal dayPrice,
            final long imageId,
            final BigDecimal ratingAvg,
            final Listing.Status status,
            final long reviewCount) {
        this.listingId = listingId;
        this.carId = carId;
        this.brand = brand;
        this.model = model;
        this.dayPrice = dayPrice;
        this.imageId = imageId;
        this.ratingAvg = ratingAvg;
        this.status = status;
        this.reviewCount = reviewCount;
    }

    public long getListingId() {
        return listingId;
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

    public long getReviewCount() {
        return reviewCount;
    }
}
