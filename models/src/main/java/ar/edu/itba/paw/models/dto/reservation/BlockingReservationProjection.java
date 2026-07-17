package ar.edu.itba.paw.models.dto.reservation;

import java.time.OffsetDateTime;

import ar.edu.itba.paw.models.domain.reservation.Reservation;

/** Immutable fields needed to account for a reservation blocking availability. */
public final class BlockingReservationProjection {

    private final long id;
    private final long carId;
    private final OffsetDateTime startDate;
    private final OffsetDateTime endDate;
    private final Reservation.Status status;

    public BlockingReservationProjection(
            final long id,
            final long carId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final Reservation.Status status) {
        this.id = id;
        this.carId = carId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public long getCarId() {
        return carId;
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
