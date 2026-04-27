package ar.edu.itba.paw.models.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ar.edu.itba.paw.models.domain.Reservation;

public final class ReservationCard {

    private final long reservationId;
    private final long listingId;
    private final String brand;
    private final String model;
    private final BigDecimal dayPrice;
    private final long imageId;
    private final OffsetDateTime startDate;
    private final OffsetDateTime endDate;
    private final Reservation.Status status;

    public ReservationCard(
            final long reservationId,
            final long listingId,
            final String brand,
            final String model,
            final BigDecimal dayPrice,
            final long imageId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final Reservation.Status status) {
        this.reservationId = reservationId;
        this.listingId = listingId;
        this.brand = brand;
        this.model = model;
        this.dayPrice = dayPrice;
        this.imageId = imageId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public long getReservationId() {
        return reservationId;
    }

    public long getListingId() {
        return listingId;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public BigDecimal getDayPrice() {
        return dayPrice;
    }

    public long getImageId() {
        return imageId;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public OffsetDateTime getEndDate() {
        return endDate;
    }

    public Reservation.Status getStatus() {
        return status;
    }
}
