package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.ReservationAvailabilityLink;

/**
 * Bridge rows between reservations and the availability segments that priced each wall-calendar chunk.
 * Totals are computed in JPQL from {@code ListingAvailability.dayPrice} and covered date ranges.
 * Access is restricted to {@link ar.edu.itba.paw.services.ReservationAvailabilityServiceImpl}.
 */
public interface ReservationAvailabilityDao {

    /**
     * Persists the day-by-day breakdown for {@code reservationId}. Call within the same transaction as
     * {@link ReservationDao#createReservation}.
     */
    void insertLinks(long reservationId, List<ReservationAvailabilityLink> links);

    /**
     * {@code SUM(day_price * covered_days)} for an existing reservation from its bridge rows.
     */
    Optional<BigDecimal> sumReservationTotal(long reservationId);

    /**
     * Quotes {@code SUM(day_price * covered_days)} for {@code links} without persisting a reservation.
     */
    Optional<BigDecimal> quoteTotalFromLinks(List<ReservationAvailabilityLink> links);
}
