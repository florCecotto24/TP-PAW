package ar.edu.itba.paw.webapp.dto.rest;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import javax.ws.rs.core.UriInfo;

import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.webapp.support.ReservationRestEnums;

/**
 * REST reservation representation ({@code application/vnd.paw.reservation.v1+json}).
 */
public final class ReservationDto {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private String startDate;
    private String endDate;
    private String status;
    private BigDecimal totalPrice;
    private Boolean carReturned;
    private String paymentProofDeadlineAt;
    private String refundProofDeadlineAt;
    private Boolean paymentRefundRequired;
    private String createdAt;
    private LinksDto links;

    public ReservationDto() {
    }

    public static ReservationDto from(final Reservation reservation, final long ownerId, final UriInfo uriInfo) {
        final ReservationDto dto = new ReservationDto();
        dto.startDate = ISO_OFFSET.format(reservation.getStartDate());
        dto.endDate = ISO_OFFSET.format(reservation.getEndDate());
        dto.status = ReservationRestEnums.toRestName(reservation.getStatus());
        dto.totalPrice = reservation.getTotalPrice();
        dto.carReturned = reservation.isCarReturned();
        dto.paymentProofDeadlineAt = reservation.getPaymentProofDeadlineAt()
                .map(ISO_OFFSET::format)
                .orElse(null);
        dto.refundProofDeadlineAt = reservation.getRefundProofDeadlineAt()
                .map(ISO_OFFSET::format)
                .orElse(null);
        dto.paymentRefundRequired = reservation.isPaymentRefundRequired();
        dto.createdAt = reservation.getCreatedAt() == null
                ? null
                : ISO_OFFSET.format(reservation.getCreatedAt());
        dto.links = ReservationLinks.forReservation(reservation, ownerId, uriInfo);
        return dto;
    }

    public static ReservationDto fromCard(final ReservationCard card, final UriInfo uriInfo) {
        final ReservationDto dto = new ReservationDto();
        dto.startDate = ISO_OFFSET.format(card.getStartDate());
        dto.endDate = ISO_OFFSET.format(card.getEndDate());
        dto.status = ReservationRestEnums.toRestName(card.getStatus());
        dto.totalPrice = card.getTotalPrice();
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

    public Boolean getCarReturned() {
        return carReturned;
    }

    public void setCarReturned(final Boolean carReturned) {
        this.carReturned = carReturned;
    }

    public String getPaymentProofDeadlineAt() {
        return paymentProofDeadlineAt;
    }

    public void setPaymentProofDeadlineAt(final String paymentProofDeadlineAt) {
        this.paymentProofDeadlineAt = paymentProofDeadlineAt;
    }

    public String getRefundProofDeadlineAt() {
        return refundProofDeadlineAt;
    }

    public void setRefundProofDeadlineAt(final String refundProofDeadlineAt) {
        this.refundProofDeadlineAt = refundProofDeadlineAt;
    }

    public Boolean getPaymentRefundRequired() {
        return paymentRefundRequired;
    }

    public void setPaymentRefundRequired(final Boolean paymentRefundRequired) {
        this.paymentRefundRequired = paymentRefundRequired;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt;
    }

    public LinksDto getLinks() {
        return links;
    }

    public void setLinks(final LinksDto links) {
        this.links = links;
    }
}
