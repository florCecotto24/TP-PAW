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
    private String carReturnedAt;
    private String paymentProofDeadlineAt;
    private String refundProofDeadlineAt;
    private Boolean paymentRefundRequired;
    private Boolean hasPaymentReceipt;
    private Boolean hasRefundReceipt;
    private String ownerCbu;
    private String pickupStreet;
    private String pickupNumber;
    private String pickupNeighborhood;
    private String checkInTime;
    private String checkOutTime;
    private String createdAt;
    private LinksDto links;

    public ReservationDto() {
    }

    public static Builder builder(
            final Reservation reservation,
            final long ownerId,
            final UriInfo uriInfo) {
        return new Builder(reservation, ownerId, uriInfo);
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

    public static final class Builder {

        private final Reservation reservation;
        private final long ownerId;
        private final UriInfo uriInfo;
        private boolean hasPaymentReceipt;
        private boolean hasRefundReceipt;
        private String ownerCbu;
        private String pickupStreet;
        private String pickupNumber;
        private String pickupNeighborhood;
        private String checkInTime;
        private String checkOutTime;

        private Builder(final Reservation reservation, final long ownerId, final UriInfo uriInfo) {
            this.reservation = reservation;
            this.ownerId = ownerId;
            this.uriInfo = uriInfo;
        }

        public Builder hasPaymentReceipt(final boolean hasPaymentReceipt) {
            this.hasPaymentReceipt = hasPaymentReceipt;
            return this;
        }

        public Builder hasRefundReceipt(final boolean hasRefundReceipt) {
            this.hasRefundReceipt = hasRefundReceipt;
            return this;
        }

        public Builder ownerCbu(final String ownerCbu) {
            this.ownerCbu = ownerCbu;
            return this;
        }

        public Builder pickupStreet(final String pickupStreet) {
            this.pickupStreet = pickupStreet;
            return this;
        }

        public Builder pickupNumber(final String pickupNumber) {
            this.pickupNumber = pickupNumber;
            return this;
        }

        public Builder pickupNeighborhood(final String pickupNeighborhood) {
            this.pickupNeighborhood = pickupNeighborhood;
            return this;
        }

        public Builder checkInTime(final String checkInTime) {
            this.checkInTime = checkInTime;
            return this;
        }

        public Builder checkOutTime(final String checkOutTime) {
            this.checkOutTime = checkOutTime;
            return this;
        }

        public ReservationDto build() {
            final ReservationDto dto = new ReservationDto();
            dto.startDate = ISO_OFFSET.format(reservation.getStartDate());
            dto.endDate = ISO_OFFSET.format(reservation.getEndDate());
            dto.status = ReservationRestEnums.toRestName(reservation.getStatus());
            dto.totalPrice = reservation.getTotalPrice();
            dto.carReturned = reservation.isCarReturned();
            dto.carReturnedAt = reservation.getCarReturnedAt()
                    .map(ISO_OFFSET::format)
                    .orElse(null);
            dto.paymentProofDeadlineAt = reservation.getPaymentProofDeadlineAt()
                    .map(ISO_OFFSET::format)
                    .orElse(null);
            dto.refundProofDeadlineAt = reservation.getRefundProofDeadlineAt()
                    .map(ISO_OFFSET::format)
                    .orElse(null);
            dto.paymentRefundRequired = reservation.isPaymentRefundRequired();
            dto.hasPaymentReceipt = hasPaymentReceipt;
            dto.hasRefundReceipt = hasRefundReceipt;
            dto.ownerCbu = ownerCbu;
            dto.pickupStreet = pickupStreet;
            dto.pickupNumber = pickupNumber;
            dto.pickupNeighborhood = pickupNeighborhood;
            dto.checkInTime = checkInTime;
            dto.checkOutTime = checkOutTime;
            dto.createdAt = reservation.getCreatedAt() == null
                    ? null
                    : ISO_OFFSET.format(reservation.getCreatedAt());
            dto.links = ReservationLinks.reservation(reservation, ownerId, uriInfo).build();
            return dto;
        }
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

    public String getCarReturnedAt() {
        return carReturnedAt;
    }

    public void setCarReturnedAt(final String carReturnedAt) {
        this.carReturnedAt = carReturnedAt;
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

    public Boolean getHasPaymentReceipt() {
        return hasPaymentReceipt;
    }

    public void setHasPaymentReceipt(final Boolean hasPaymentReceipt) {
        this.hasPaymentReceipt = hasPaymentReceipt;
    }

    public Boolean getHasRefundReceipt() {
        return hasRefundReceipt;
    }

    public void setHasRefundReceipt(final Boolean hasRefundReceipt) {
        this.hasRefundReceipt = hasRefundReceipt;
    }

    public String getOwnerCbu() {
        return ownerCbu;
    }

    public void setOwnerCbu(final String ownerCbu) {
        this.ownerCbu = ownerCbu;
    }

    public String getPickupStreet() {
        return pickupStreet;
    }

    public void setPickupStreet(final String pickupStreet) {
        this.pickupStreet = pickupStreet;
    }

    public String getPickupNumber() {
        return pickupNumber;
    }

    public void setPickupNumber(final String pickupNumber) {
        this.pickupNumber = pickupNumber;
    }

    public String getPickupNeighborhood() {
        return pickupNeighborhood;
    }

    public void setPickupNeighborhood(final String pickupNeighborhood) {
        this.pickupNeighborhood = pickupNeighborhood;
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
}
