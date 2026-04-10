package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Reservation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

public interface ReservationService {

    Reservation createReservation(
            long riderId,
            long listingId,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Reservation.Status status);

    Optional<Reservation> getReservationById(long id);

    Optional<String> normalizeClientReservationTotal(String reservationTotal);

    Optional<String> reservationTotalDisplay(Long listingId, String fromDateTime, String untilDateTime);

    /**
     * Rider booking from the web form: validate {@code riderId}, listing/dates/availability, then {@link #createReservation}.
     *
     * @throws ar.edu.itba.paw.exception.reservation.RiderReservationException for validation errors (message key in {@link ar.edu.itba.paw.exception.MessageKeys})
     * @throws ar.edu.itba.paw.exception.reservation.ReservationConflictException when the interval overlaps existing reservations
     */
    Reservation submitRiderReservation(
            long riderId,
            Long listingId,
            Long availabilityId,
            String fromDateTime,
            String untilDateTime);

    Optional<BigDecimal> calculateTotal(long listingId, OffsetDateTime startDate, OffsetDateTime endDate);

    long calculateBillableDays(OffsetDateTime startDate, OffsetDateTime endDate);
}
