package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Reservation;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface ReservationDao {

    Reservation createReservation(
            long riderId,
            long listingId,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Reservation.Status status);

    Optional<Reservation> getReservationById(long id);
}
