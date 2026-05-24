package ar.edu.itba.paw.models.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Min / max / average day prices for active cars with the same brand and model. */
public final class CarPriceMarketInsight {

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

    private static BigDecimal scaleMoney(final BigDecimal value) {
        Objects.requireNonNull(value, "value");
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
