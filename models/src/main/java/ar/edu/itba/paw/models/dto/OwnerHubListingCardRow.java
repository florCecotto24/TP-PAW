package ar.edu.itba.paw.models.dto;

import java.math.BigDecimal;
import java.util.Objects;

import ar.edu.itba.paw.models.domain.Listing;

/**
 * Owner “my listings” hub card row (mirrors fields used by {@code myListings.jsp} and the former {@code VehicleCardView}
 * owner mapping).
 */
public final class OwnerHubListingCardRow {

    private final long listingId;
    private final String brand;
    private final String model;
    private final BigDecimal price;
    private final long imageId;
    private final String statusKey;
    private final BigDecimal ratingAvg;

    public OwnerHubListingCardRow(
            final long listingId,
            final String brand,
            final String model,
            final BigDecimal price,
            final long imageId,
            final String statusKey,
            final BigDecimal ratingAvg) {
        this.listingId = listingId;
        this.brand = Objects.requireNonNull(brand, "brand");
        this.model = Objects.requireNonNull(model, "model");
        this.price = Objects.requireNonNull(price, "price");
        this.imageId = imageId;
        this.statusKey = statusKey;
        this.ratingAvg = ratingAvg;
    }

    public static OwnerHubListingCardRow fromOwnerListingCard(final ListingCard card) {
        final String sk = card.getStatus().map(Listing.Status::name).orElse(null);
        return new OwnerHubListingCardRow(
                card.getListingId(),
                card.getBrand(),
                card.getModel(),
                card.getDayPrice(),
                card.getImageId(),
                sk,
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
