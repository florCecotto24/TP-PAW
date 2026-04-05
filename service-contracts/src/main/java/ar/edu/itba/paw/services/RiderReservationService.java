package ar.edu.itba.paw.services;

import java.util.Optional;

import ar.edu.itba.paw.models.Reservation;


public interface RiderReservationService {

    Optional<String> normalizeClientReservationTotal(String reservationTotal);

    Optional<String> reservationTotalDisplay(Long listingId, String fromDateTime, String untilDateTime);

    /**
     * @throws RiderReservationException for validation errors (user-facing message)
     * @throws ReservationConflictException when the interval overlaps existing reservations
     */
    Reservation submitRiderReservation(
            String email,
            String name,
            String surname,
            Long listingId,
            Long availabilityId,
            String fromDateTime,
            String untilDateTime);
}
