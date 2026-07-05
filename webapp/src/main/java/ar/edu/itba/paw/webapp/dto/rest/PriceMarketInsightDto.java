package ar.edu.itba.paw.webapp.dto.rest;

import java.math.BigDecimal;

import ar.edu.itba.paw.models.dto.car.CarPriceMarketInsight;

/** REST view of comparable day-price statistics for a brand/model pair. */
public final class PriceMarketInsightDto {

    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal averagePrice;
    private long sampleCount;

    public PriceMarketInsightDto() {
    }

    public static PriceMarketInsightDto from(final CarPriceMarketInsight insight) {
        final PriceMarketInsightDto dto = new PriceMarketInsightDto();
        dto.minPrice = insight.getMinPrice();
        dto.maxPrice = insight.getMaxPrice();
        dto.averagePrice = insight.getAveragePrice();
        dto.sampleCount = insight.getSampleCount();
        return dto;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(final BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(final BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(final BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }

    public long getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(final long sampleCount) {
        this.sampleCount = sampleCount;
    }
}
