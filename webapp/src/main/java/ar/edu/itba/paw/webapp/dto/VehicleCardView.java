package ar.edu.itba.paw.webapp.dto;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.car.CarPriceMarketInsight;
import ar.edu.itba.paw.models.dto.car.PriceMarketPosition;

/**
 * Car teaser for home and search grids, optionally enriched with status and average rating.
 * Instances are built through {@link #builder()} or one of the {@code fromCarCard*} factories;
 * the constructor is intentionally private (Effective Java, Item 2 — Builder pattern).
 */
public final class VehicleCardView {
    private final long carId;
    private final String brand;
    private final String model;
    private final BigDecimal price;
    private final long imageId;
    private final String statusKey;
    private final BigDecimal ratingAvg;
    private final long reviewCount;
    private final PriceMarketPosition priceMarketPosition;
    private final BigDecimal marketAveragePrice;
    private final long marketSampleCount;
    private final int minimumRentalDays;
    /** Owner of the underlying car, when known; used to hide the favorite heart on own cars. */
    private final Long ownerId;
    /** {@code true} when the heart button should be rendered (logged-in viewer & not owner). */
    private final boolean favoritable;
    /** {@code true} when the viewer has already favorited this car. */
    private final boolean favorited;

    private VehicleCardView(final Builder b) {
        this.carId = b.carId;
        this.brand = b.brand;
        this.model = b.model;
        this.price = b.price;
        this.imageId = b.imageId;
        this.statusKey = b.statusKey;
        this.ratingAvg = b.ratingAvg;
        this.reviewCount = b.reviewCount;
        this.priceMarketPosition = b.priceMarketPosition;
        this.marketAveragePrice = b.marketAveragePrice;
        this.marketSampleCount = b.marketSampleCount;
        this.minimumRentalDays = b.minimumRentalDays;
        this.ownerId = b.ownerId;
        this.favoritable = b.favoritable;
        this.favorited = b.favorited;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Explore / search grid from a car card (no listing required). */
    public static VehicleCardView fromCarCard(final CarCard card) {
        return baseBuilderFromCard(card).build();
    }

    /** Consumer browse card with optional price market badge fields. */
    public static VehicleCardView fromCarCard(
            final CarCard card,
            final PriceMarketPosition position,
            final CarPriceMarketInsight insight) {
        if (position == null || insight == null) {
            return fromCarCard(card);
        }
        return baseBuilderFromCard(card)
                .priceMarketPosition(position)
                .marketAveragePrice(insight.getAveragePrice())
                .marketSampleCount(insight.getSampleCount())
                .build();
    }

    /**
     * Returns a copy of this view with the favorite flags resolved against the current viewer:
     * the heart is rendered when the viewer is logged in and is not the owner; the heart is
     * shown filled when the viewer has already favorited the car.
     */
    public VehicleCardView withFavoriteState(final Long viewerUserId, final Set<Long> favoritedCarIds) {
        final boolean isOwn = viewerUserId != null && ownerId != null && ownerId.equals(viewerUserId);
        final boolean canFavorite = viewerUserId != null && !isOwn;
        final boolean isFavorited = canFavorite && favoritedCarIds != null && favoritedCarIds.contains(carId);
        return toBuilder()
                .favoritable(canFavorite)
                .favorited(isFavorited)
                .build();
    }

    private static Builder baseBuilderFromCard(final CarCard card) {
        return builder()
                .carId(card.getCarId())
                .brand(card.getBrand())
                .model(card.getModel())
                .price(card.getDayPrice())
                .imageId(card.getImageId())
                .ratingAvg(card.getRatingAvg())
                .minimumRentalDays(card.getMinimumRentalDays())
                .ownerId(card.getOwnerId());
    }

    private Builder toBuilder() {
        return builder()
                .carId(carId)
                .brand(brand)
                .model(model)
                .price(price)
                .imageId(imageId)
                .statusKey(statusKey)
                .ratingAvg(ratingAvg)
                .reviewCount(reviewCount)
                .priceMarketPosition(priceMarketPosition)
                .marketAveragePrice(marketAveragePrice)
                .marketSampleCount(marketSampleCount)
                .minimumRentalDays(minimumRentalDays)
                .ownerId(ownerId)
                .favoritable(favoritable)
                .favorited(favorited);
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

    public String getStatusKey() {
        return statusKey;
    }

    public BigDecimal getRatingAvg() {
        return ratingAvg;
    }

    public long getReviewCount() {
        return reviewCount;
    }

    /** CSS modifier suffix for {@code carcard-price-market-badge--*}, e.g. {@code below_market}. */
    public String getPriceMarketPositionModifier() {
        return priceMarketPosition == null ? null : priceMarketPosition.name().toLowerCase();
    }

    public BigDecimal getMarketAveragePrice() {
        return marketAveragePrice;
    }

    public long getMarketSampleCount() {
        return marketSampleCount;
    }

    public int getMinimumRentalDays() {
        return minimumRentalDays;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public boolean isFavoritable() {
        return favoritable;
    }

    public boolean isFavorited() {
        return favorited;
    }

    public static final class Builder {
        private long carId;
        private String brand;
        private String model;
        private BigDecimal price;
        private long imageId;
        private String statusKey;
        private BigDecimal ratingAvg;
        private long reviewCount;
        private PriceMarketPosition priceMarketPosition;
        private BigDecimal marketAveragePrice;
        private long marketSampleCount;
        private int minimumRentalDays = 1;
        private Long ownerId;
        private boolean favoritable;
        private boolean favorited;

        private Builder() {
        }

        public Builder carId(final long carId) {
            this.carId = carId;
            return this;
        }

        public Builder brand(final String brand) {
            this.brand = brand;
            return this;
        }

        public Builder model(final String model) {
            this.model = model;
            return this;
        }

        public Builder price(final BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder imageId(final long imageId) {
            this.imageId = imageId;
            return this;
        }

        public Builder statusKey(final String statusKey) {
            this.statusKey = statusKey;
            return this;
        }

        public Builder ratingAvg(final BigDecimal ratingAvg) {
            this.ratingAvg = ratingAvg;
            return this;
        }

        public Builder reviewCount(final long reviewCount) {
            this.reviewCount = reviewCount;
            return this;
        }

        public Builder priceMarketPosition(final PriceMarketPosition priceMarketPosition) {
            this.priceMarketPosition = priceMarketPosition;
            return this;
        }

        public Builder marketAveragePrice(final BigDecimal marketAveragePrice) {
            this.marketAveragePrice = marketAveragePrice;
            return this;
        }

        public Builder marketSampleCount(final long marketSampleCount) {
            this.marketSampleCount = marketSampleCount;
            return this;
        }

        public Builder minimumRentalDays(final int minimumRentalDays) {
            this.minimumRentalDays = Math.max(minimumRentalDays, 1);
            return this;
        }

        public Builder ownerId(final Long ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public Builder favoritable(final boolean favoritable) {
            this.favoritable = favoritable;
            return this;
        }

        public Builder favorited(final boolean favorited) {
            this.favorited = favorited;
            return this;
        }

        public VehicleCardView build() {
            Objects.requireNonNull(brand, "brand");
            Objects.requireNonNull(model, "model");
            return new VehicleCardView(this);
        }
    }
}
