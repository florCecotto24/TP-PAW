package ar.edu.itba.paw.webapp.dto.rest;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.webapp.support.ReservationRestEnums;

/**
 * Teaser representation for reservation collections
 * ({@code application/vnd.paw.reservation.summary.v1+json}).
 */
public final class ReservationSummaryDto {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private String startDate;
    private String endDate;
    private String status;
    private BigDecimal totalPrice;
    private String brandName;
    private String modelName;
    private LinksDto links;

    public ReservationSummaryDto() {
    }

    public static ReservationSummaryDto fromCard(final ReservationCard card, final UriInfo uriInfo) {
        final ReservationSummaryDto dto = new ReservationSummaryDto();
        dto.startDate = ISO_OFFSET.format(card.getStartDate());
        dto.endDate = ISO_OFFSET.format(card.getEndDate());
        dto.status = ReservationRestEnums.toRestName(card.getStatus());
        dto.totalPrice = card.getTotalPrice();
        dto.brandName = card.getBrand();
        dto.modelName = card.getModel();
        dto.links = ReservationLinks.forCard(card, uriInfo);
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

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(final BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
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

    public LinksDto getLinks() {
        return links;
    }

    public void setLinks(final LinksDto links) {
        this.links = links;
    }
}
