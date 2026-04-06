package ar.edu.itba.paw.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.Reservation;

public interface ReservationDao {

    boolean hasActiveOverlap(long listingId, OffsetDateTime startDate, OffsetDateTime endDate);

    List<Reservation> findBlockingByListingId(long listingId);

    Reservation createReservation(
            long riderId,
            long listingId,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Reservation.Status status);

    Optional<Reservation> getReservationById(long id);
}
