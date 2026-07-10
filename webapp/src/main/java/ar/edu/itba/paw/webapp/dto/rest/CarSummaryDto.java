package ar.edu.itba.paw.webapp.dto.rest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.car.ConsumerCarCardMarketContext;
import ar.edu.itba.paw.models.dto.car.PriceMarketPosition;
import ar.edu.itba.paw.webapp.support.CarRestEnums;

/**
 * Teaser representation for car collections ({@code application/vnd.paw.car.summary.v1+json}).
 */
public final class CarSummaryDto {

    private String brandName;
    private String modelName;
    private String status;
    private int minimumRentalDays;
    private BigDecimal ratingAvg;
    private BigDecimal dayPrice;
    private boolean modelValidated;
    private String priceMarketPositionModifier;
    private BigDecimal marketAveragePrice;
    private Long marketSampleCount;
    private LinksDto links;

    public CarSummaryDto() {
    }

    /** Teaser from a hydrated car entity (same URI as {@link CarDto}, different MIME). */
    public static CarSummaryDto from(final Car car, final UriInfo uriInfo) {
        final CarSummaryDto dto = new CarSummaryDto();
        dto.brandName = car.getBrand();
        dto.modelName = car.getModel();
        dto.status = CarRestEnums.toRestName(car.getStatus());
        dto.minimumRentalDays = car.getMinimumRentalDays();
        dto.ratingAvg = car.getRatingAvg().orElse(null);
        dto.dayPrice = null;
        dto.modelValidated = !car.isModelPendingValidation();
        dto.links = CarLinks.forCar(car, uriInfo);
        return dto;
    }

    public static CarSummaryDto fromCarCard(
            final CarCard card,
            final UriInfo uriInfo,
            final ConsumerCarCardMarketContext marketContext) {
        final CarSummaryDto dto = new CarSummaryDto();
        dto.brandName = card.getBrand();
        dto.modelName = card.getModel();
        dto.status = card.getStatus() == null
                ? null
                : CarRestEnums.toRestName(card.getStatus());
        dto.minimumRentalDays = card.getMinimumRentalDays();
        dto.ratingAvg = card.getRatingAvg();
        dto.dayPrice = card.getDayPrice();
        dto.modelValidated = !card.isModelPendingValidation();
        dto.links = CarLinks.forCarCard(card, uriInfo);
        applyConsumerMarketContext(dto, marketContext);
        return dto;
    }

    public static List<CarSummaryDto> fromConsumerBrowseCarCards(
            final List<CarCard> cards,
            final UriInfo uriInfo,
            final Map<Long, ConsumerCarCardMarketContext> marketContexts) {
        return cards.stream()
                .map(card -> fromCarCard(card, uriInfo, marketContexts.get(card.getCarId())))
                .collect(Collectors.toList());
    }

    private static void applyConsumerMarketContext(
            final CarSummaryDto dto, final ConsumerCarCardMarketContext marketContext) {
        if (marketContext == null) {
            return;
        }
        final PriceMarketPosition position = marketContext.getPosition();
        dto.priceMarketPositionModifier = position.name().toLowerCase(Locale.ROOT);
        dto.marketAveragePrice = marketContext.getInsight().getAveragePrice();
        dto.marketSampleCount = marketContext.getInsight().getSampleCount();
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(final String brandName) {
        this.brandName = brandName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(final String modelName) {
        this.modelName = modelName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public int getMinimumRentalDays() {
        return minimumRentalDays;
    }

    public void setMinimumRentalDays(final int minimumRentalDays) {
        this.minimumRentalDays = minimumRentalDays;
    }

    public BigDecimal getRatingAvg() {
        return ratingAvg;
    }

    public void setRatingAvg(final BigDecimal ratingAvg) {
        this.ratingAvg = ratingAvg;
    }

    public BigDecimal getDayPrice() {
        return dayPrice;
    }

    public void setDayPrice(final BigDecimal dayPrice) {
        this.dayPrice = dayPrice;
    }

    public boolean isModelValidated() {
        return modelValidated;
    }

    public void setModelValidated(final boolean modelValidated) {
        this.modelValidated = modelValidated;
    }

    public String getPriceMarketPositionModifier() {
        return priceMarketPositionModifier;
    }

    public void setPriceMarketPositionModifier(final String priceMarketPositionModifier) {
        this.priceMarketPositionModifier = priceMarketPositionModifier;
    }

    public BigDecimal getMarketAveragePrice() {
        return marketAveragePrice;
    }

    public void setMarketAveragePrice(final BigDecimal marketAveragePrice) {
        this.marketAveragePrice = marketAveragePrice;
    }

    public Long getMarketSampleCount() {
        return marketSampleCount;
    }

    public void setMarketSampleCount(final Long marketSampleCount) {
        this.marketSampleCount = marketSampleCount;
    }

    public LinksDto getLinks() {
        return links;
    }

    public void setLinks(final LinksDto links) {
        this.links = links;
    }
}
