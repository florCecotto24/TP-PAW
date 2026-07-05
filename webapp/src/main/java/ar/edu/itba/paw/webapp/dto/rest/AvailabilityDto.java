package ar.edu.itba.paw.webapp.dto.rest;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;
import ar.edu.itba.paw.webapp.support.CarRestEnums;
import ar.edu.itba.paw.webapp.util.RestUriUtils;

/** REST availability period ({@code application/vnd.paw.availability.v1+json}). */
public final class AvailabilityDto {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private String startDate;
    private String endDate;
    private BigDecimal dayPrice;
    private String startPointStreet;
    private String startPointNumber;
    private String checkInTime;
    private String checkOutTime;
    private String kind;
    private LinksDto links;

    public AvailabilityDto() {
    }

    public static AvailabilityDto from(final CarAvailability availability, final UriInfo uriInfo) {
        final AvailabilityDto dto = new AvailabilityDto();
        dto.startDate = ISO_DATE.format(availability.getStartInclusive());
        dto.endDate = ISO_DATE.format(availability.getEndInclusive());
        dto.dayPrice = availability.getDayPriceValue();
        dto.startPointStreet = availability.getStartPointStreet();
        dto.startPointNumber = availability.getStartPointNumber().orElse(null);
        dto.checkInTime = availability.getCheckInTime().toString();
        dto.checkOutTime = availability.getCheckOutTime().toString();
        dto.kind = CarRestEnums.toRestName(availability.getKind());
        final long carId = availability.getCarId();
        dto.links = LinksDto.ofSelf(
                        RestUriUtils.carAvailabilityUri(uriInfo, carId, availability.getId()).toString())
                .withRelated("car", RestUriUtils.carUri(uriInfo, carId).toString());
        availability.getNeighborhoodId()
                .map(id -> RestUriUtils.neighborhoodUri(uriInfo, id).toString())
                .ifPresent(uri -> dto.links.withRelated("neighborhood", uri));
        return dto;
    }

    public static AvailabilityDto fromSegment(
            final BookableSegmentProjection segment,
            final long carId,
            final UriInfo uriInfo) {
        final AvailabilityDto dto = new AvailabilityDto();
        dto.startDate = ISO_DATE.format(segment.getFrom());
        dto.endDate = ISO_DATE.format(segment.getTo());
        dto.dayPrice = segment.getDayPrice();
        dto.startPointStreet = segment.getPublicLocation();
        dto.startPointNumber = null;
        dto.checkInTime = segment.getCheckInTime().toString();
        dto.checkOutTime = segment.getCheckOutTime().toString();
        dto.kind = CarRestEnums.toRestName(CarAvailability.Kind.OFFERED);
        dto.links = LinksDto.ofSelf(
                        RestUriUtils.carAvailabilityRangeUri(uriInfo, carId, dto.startDate, dto.endDate)
                                .toString())
                .withRelated("car", RestUriUtils.carUri(uriInfo, carId).toString());
        final Long neighborhoodId = segment.getNeighborhoodId();
        if (neighborhoodId != null) {
            dto.links.withRelated("neighborhood",
                    RestUriUtils.neighborhoodUri(uriInfo, neighborhoodId).toString());
        }
        return dto;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(final String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(final String endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getDayPrice() {
        return dayPrice;
    }

    public void setDayPrice(final BigDecimal dayPrice) {
        this.dayPrice = dayPrice;
    }

    public String getStartPointStreet() {
        return startPointStreet;
    }

    public void setStartPointStreet(final String startPointStreet) {
        this.startPointStreet = startPointStreet;
    }

    public String getStartPointNumber() {
        return startPointNumber;
    }

    public void setStartPointNumber(final String startPointNumber) {
        this.startPointNumber = startPointNumber;
    }

    public String getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(final String checkInTime) {
        this.checkInTime = checkInTime;
    }

    public String getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(final String checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(final String kind) {
        this.kind = kind;
    }

    public LinksDto getLinks() {
        return links;
    }

    public void setLinks(final LinksDto links) {
        this.links = links;
    }
}
