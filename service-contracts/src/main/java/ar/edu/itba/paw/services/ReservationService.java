package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Reservation;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface ReservationService {

    Reservation createReservation(
            long riderId,
            long listingId,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String deliveryLocation);

    Optional<Reservation> getReservationById(long id);
}
