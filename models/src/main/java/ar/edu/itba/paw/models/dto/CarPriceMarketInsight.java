package ar.edu.itba.paw.models.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

/** Min / max / average day prices for active cars with the same brand and model. */
public final class CarPriceMarketInsight {

    private static final BigDecimal BELOW_MARKET_THRESHOLD = new BigDecimal("0.90");
    private static final BigDecimal ABOVE_MARKET_THRESHOLD = new BigDecimal("1.10");

    private final BigDecimal minPrice;
    private final BigDecimal maxPrice;
    private final BigDecimal averagePrice;
    private final long sampleCount;

    public CarPriceMarketInsight(
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final BigDecimal averagePrice,
            final long sampleCount) {
        this.minPrice = scaleMoney(minPrice);
        this.maxPrice = scaleMoney(maxPrice);
        this.averagePrice = scaleMoney(averagePrice);
        this.sampleCount = sampleCount;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public long getSampleCount() {
        return sampleCount;
    }

    /**
     * Classifies {@code dayPrice} against {@link #getAveragePrice()} when at least two comparable cars exist.
     */
    public Optional<PriceMarketPosition> classifyDayPrice(final BigDecimal dayPrice) {
        if (dayPrice == null || averagePrice == null || sampleCount < 2) {
            return Optional.empty();
        }
        final BigDecimal scaledDayPrice = dayPrice.setScale(2, RoundingMode.HALF_UP);
        final BigDecimal lowBound = averagePrice.multiply(BELOW_MARKET_THRESHOLD)
                .setScale(2, RoundingMode.HALF_UP);
        final BigDecimal highBound = averagePrice.multiply(ABOVE_MARKET_THRESHOLD)
                .setScale(2, RoundingMode.HALF_UP);
        if (scaledDayPrice.compareTo(lowBound) <= 0) {
            return Optional.of(PriceMarketPosition.BELOW_MARKET);
        }
        if (scaledDayPrice.compareTo(highBound) <= 0) {
            return Optional.of(PriceMarketPosition.AT_MARKET);
        }
        return Optional.of(PriceMarketPosition.ABOVE_MARKET);
    }

    private static BigDecimal scaleMoney(final BigDecimal value) {
        Objects.requireNonNull(value, "value");
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
