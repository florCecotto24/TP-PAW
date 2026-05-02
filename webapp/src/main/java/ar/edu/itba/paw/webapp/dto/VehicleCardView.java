package ar.edu.itba.paw.webapp.dto;

import java.math.BigDecimal;

import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.dto.ListingCard;

/** Listing teaser for home and search grids, optionally enriched with status and average rating. */
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

    /**
     * Explore / search / similar listings: no listing status on the card grid.
     */
    public static VehicleCardView fromListingCard(final ListingCard card) {
        return buildFromListingCard(card, null);
    }

    /**
     * Owner “My listings” grid: status badge uses {@link Listing.Status} name.
     */
    public static VehicleCardView fromOwnerListingCard(final ListingCard card) {
        return buildFromListingCard(
                card,
                card.getStatus().map(Listing.Status::name).orElse(null));
    }

    private static VehicleCardView buildFromListingCard(
            final ListingCard card,
            final String statusKey) {
        return new VehicleCardView(
                card.getListingId(),
                card.getBrand(),
                card.getModel(),
                card.getDayPrice(),
                card.getImageId(),
                statusKey,
                card.getRatingAvg().orElse(null));
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

