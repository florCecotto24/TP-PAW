package ar.edu.itba.paw.models.dto.reservation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ar.edu.itba.paw.models.domain.reservation.Reservation;

/** Reservation with car vehicle summary, frozen total and UTC dates for "my reservations" hub queries. */
public final class ReservationCard {

    private final long reservationId;
    private final long carId;
    private final String brand;
    private final String model;
    private final BigDecimal totalPrice;
    private final long imageId;
    private final OffsetDateTime startDate;
    private final OffsetDateTime endDate;
    private final Reservation.Status status;

    public ReservationCard(
            final long reservationId,
            final long carId,
            final String brand,
            final String model,
            final BigDecimal totalPrice,
            final long imageId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final Reservation.Status status) {
        this.reservationId = reservationId;
        this.carId = carId;
        this.brand = brand;
        this.model = model;
        this.totalPrice = totalPrice;
        this.imageId = imageId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public long getReservationId() {
        return reservationId;
    }

    public long getCarId() {
        return carId;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
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
