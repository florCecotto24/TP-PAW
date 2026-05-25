package ar.edu.itba.paw.webapp.dto;

import java.math.BigDecimal;

import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.CarPriceMarketInsight;
import ar.edu.itba.paw.models.dto.PriceMarketPosition;

/** Car teaser for home and search grids, optionally enriched with status and average rating. */
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

    public VehicleCardView(
            final long carId,
            final String brand,
            final String model,
            final BigDecimal price,
            final long imageId) {
        this(carId, brand, model, price, imageId, null, null, 0, null, null, 0L);
    }

    public VehicleCardView(
            final long carId,
            final String brand,
            final String model,
            final BigDecimal price,
            final long imageId,
            final String statusKey) {
        this(carId, brand, model, price, imageId, statusKey, null, 0, null, null, 0L);
    }

    public VehicleCardView(
            final long carId,
            final String brand,
            final String model,
            final BigDecimal price,
            final long imageId,
            final String statusKey,
            final BigDecimal ratingAvg) {
        this(carId, brand, model, price, imageId, statusKey, ratingAvg, 0, null, null, 0L);
    }

    public VehicleCardView(
            final long carId,
            final String brand,
            final String model,
            final BigDecimal price,
            final long imageId,
            final String statusKey,
            final BigDecimal ratingAvg,
            final long reviewCount) {
        this(carId, brand, model, price, imageId, statusKey, ratingAvg, reviewCount, null, null, 0L);
    }

    public VehicleCardView(
            final long carId,
            final String brand,
            final String model,
            final BigDecimal price,
            final long imageId,
            final String statusKey,
            final BigDecimal ratingAvg,
            final long reviewCount,
            final PriceMarketPosition priceMarketPosition,
            final BigDecimal marketAveragePrice,
            final long marketSampleCount) {
        this.carId = carId;
        this.brand = brand;
        this.model = model;
        this.price = price;
        this.imageId = imageId;
        this.statusKey = statusKey;
        this.ratingAvg = ratingAvg;
        this.reviewCount = reviewCount;
        this.priceMarketPosition = priceMarketPosition;
        this.marketAveragePrice = marketAveragePrice;
        this.marketSampleCount = marketSampleCount;
    }

    /**
     * Explore / search grid from a car card (no listing required).
     */
    public static VehicleCardView fromCarCard(final CarCard card) {
        return new VehicleCardView(
                card.getCarId(),
                card.getBrand(),
                card.getModel(),
                card.getDayPrice(),
                card.getImageId(),
                null,
                card.getRatingAvg(),
                0);
    }

    /**
     * Consumer browse card with optional price market badge fields.
     */
    public static VehicleCardView fromCarCard(
            final CarCard card,
            final PriceMarketPosition position,
            final CarPriceMarketInsight insight) {
        if (position == null || insight == null) {
            return fromCarCard(card);
        }
        return new VehicleCardView(
                card.getCarId(),
                card.getBrand(),
                card.getModel(),
                card.getDayPrice(),
                card.getImageId(),
                null,
                card.getRatingAvg(),
                0,
                position,
                insight.getAveragePrice(),
                insight.getSampleCount());
    }

    /**
     * Owner "My cars" grid from a car card with status.
     */
    public static VehicleCardView fromOwnerCarCard(final CarCard card) {
        return new VehicleCardView(
                card.getCarId(),
                card.getBrand(),
                card.getModel(),
                card.getDayPrice(),
                card.getImageId(),
                card.getStatusKey(),
                card.getRatingAvg(),
                0);
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

    public PriceMarketPosition getPriceMarketPosition() {
        return priceMarketPosition;
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
}
