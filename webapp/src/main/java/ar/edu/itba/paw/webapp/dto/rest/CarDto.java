package ar.edu.itba.paw.webapp.dto.rest;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
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
 * REST car representation ({@code application/vnd.paw.car.v1+json}).
 * Favorites and browse endpoints may populate only the fields available in {@link CarCard}.
 */
public final class CarDto {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private String plate;
    private Integer year;
    private String powertrain;
    private String transmission;
    private String type;
    private String status;
    private String description;
    private int minimumRentalDays;
    private BigDecimal ratingAvg;
    private BigDecimal dayPrice;
    private String brandName;
    private String modelName;
    private boolean modelValidated;
    private boolean hasInsurance;
    private String createdAt;
    /** Consumer browse cards only: below_market, at_market, above_market. */
    private String priceMarketPositionModifier;
    private BigDecimal marketAveragePrice;
    private Long marketSampleCount;
    private LinksDto links;

    public CarDto() {
    }

    public static CarDto from(final Car car, final UriInfo uriInfo) {
        return from(car, uriInfo, false);
    }

    /**
     * @param includePlate whether to expose the licence plate (a sensitive identifier). Only owner/admin
     *                     callers may see it; public/anonymous detail views must pass {@code false}.
     */
    public static CarDto from(final Car car, final UriInfo uriInfo, final boolean includePlate) {
        final CarDto dto = new CarDto();
        dto.plate = includePlate ? car.getPlate() : null;
        dto.year = car.getYear().orElse(null);
        dto.powertrain = CarRestEnums.toRestName(car.getPowertrain());
        dto.transmission = CarRestEnums.toRestName(car.getTransmission());
        dto.type = car.getCarModel()
                .map(m -> CarRestEnums.toRestName(m.getType()))
                .orElse(null);
        dto.status = CarRestEnums.toRestName(car.getStatus());
        dto.description = car.getDescription().orElse(null);
        dto.minimumRentalDays = car.getMinimumRentalDays();
        dto.ratingAvg = car.getRatingAvg().orElse(null);
        dto.brandName = car.getBrand();
        dto.modelName = car.getModel();
        dto.modelValidated = !car.isModelPendingValidation();
        dto.hasInsurance = car.getInsuranceFileId().isPresent();
        dto.createdAt = car.getCreatedAt() == null ? null : ISO_OFFSET.format(car.getCreatedAt());
        dto.links = CarLinks.forCar(car, uriInfo);
        return dto;
    }

    public static CarDto fromCarCard(final CarCard card, final UriInfo uriInfo) {
        return fromCarCard(card, uriInfo, null);
    }

    public static CarDto fromCarCard(
            final CarCard card,
            final UriInfo uriInfo,
            final ConsumerCarCardMarketContext marketContext) {
        final CarDto dto = new CarDto();
        dto.plate = null;
        dto.year = card.getYear();
        dto.powertrain = null;
        dto.transmission = null;
        dto.type = null;
        dto.status = card.getStatus() == null
                ? null
                : card.getStatus().name().toLowerCase(Locale.ROOT);
        dto.minimumRentalDays = card.getMinimumRentalDays();
        dto.ratingAvg = card.getRatingAvg();
        dto.dayPrice = card.getDayPrice();
        dto.brandName = card.getBrand();
        dto.modelName = card.getModel();
        dto.modelValidated = !card.isModelPendingValidation();
        dto.hasInsurance = false;
        dto.links = CarLinks.forCarCard(card, uriInfo);
        applyConsumerMarketContext(dto, marketContext);
        return dto;
    }

    private static void applyConsumerMarketContext(
            final CarDto dto, final ConsumerCarCardMarketContext marketContext) {
        if (marketContext == null) {
            return;
        }
        final PriceMarketPosition position = marketContext.getPosition();
        dto.priceMarketPositionModifier = position.name().toLowerCase(Locale.ROOT);
        dto.marketAveragePrice = marketContext.getInsight().getAveragePrice();
        dto.marketSampleCount = marketContext.getInsight().getSampleCount();
    }

    /** Maps browse/favorites card rows with pre-resolved consumer market badge data. */
    public static List<CarDto> fromConsumerBrowseCarCards(
            final List<CarCard> cards,
            final UriInfo uriInfo,
            final Map<Long, ConsumerCarCardMarketContext> marketContexts) {
        return cards.stream()
                .map(card -> fromCarCard(card, uriInfo, marketContexts.get(card.getCarId())))
                .collect(Collectors.toList());
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(final String plate) {
        this.plate = plate;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(final Integer year) {
        this.year = year;
    }

    public String getPowertrain() {
        return powertrain;
    }

    public void setPowertrain(final String powertrain) {
        this.powertrain = powertrain;
    }

    public String getTransmission() {
        return transmission;
    }

    public void setTransmission(final String transmission) {
        this.transmission = transmission;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
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

    public boolean isModelValidated() {
        return modelValidated;
    }

    public void setModelValidated(final boolean modelValidated) {
        this.modelValidated = modelValidated;
    }

    public boolean getHasInsurance() {
        return hasInsurance;
    }

    public void setHasInsurance(final boolean hasInsurance) {
        this.hasInsurance = hasInsurance;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt;
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
