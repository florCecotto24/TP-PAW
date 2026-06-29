package ar.edu.itba.paw.webapp.dto.rest;

import java.math.BigDecimal;
import java.time.LocalTime;

import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;

/** Rider-facing bookable wall-day segment for car-detail date pickers. */
public final class BookableSegmentDto {

    private String from;
    private String to;
    private BigDecimal dayPrice;
    private String checkInTime;
    private String checkOutTime;
    private String location;
    private Long neighborhoodId;

    public BookableSegmentDto() {
    }

    public static BookableSegmentDto from(final BookableSegmentProjection segment) {
        final BookableSegmentDto dto = new BookableSegmentDto();
        dto.from = segment.getFrom().toString();
        dto.to = segment.getTo().toString();
        dto.dayPrice = segment.getDayPrice();
        dto.checkInTime = formatTime(segment.getCheckInTime());
        dto.checkOutTime = formatTime(segment.getCheckOutTime());
        dto.location = segment.getPublicLocation();
        dto.neighborhoodId = segment.getNeighborhoodId();
        return dto;
    }

    private static String formatTime(final LocalTime time) {
        if (time == null) {
            return null;
        }
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(final String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(final String to) {
        this.to = to;
    }

    public BigDecimal getDayPrice() {
        return dayPrice;
    }

    public void setDayPrice(final BigDecimal dayPrice) {
        this.dayPrice = dayPrice;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public Long getNeighborhoodId() {
        return neighborhoodId;
    }

    public void setNeighborhoodId(final Long neighborhoodId) {
        this.neighborhoodId = neighborhoodId;
    }
}
