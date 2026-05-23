package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.ReservationAvailabilityLink;

/**
 * Pricing breakdown rows linking a reservation to the {@code listing_availability} segments that priced each
 * wall-calendar chunk. Implementations use {@code ReservationAvailabilityDao} only.
 */
public interface ReservationAvailabilityService {

    /** Persists the breakdown for {@code reservationId}; join the caller’s transaction when one is active. */
    void insertLinks(long reservationId, List<ReservationAvailabilityLink> links);

    /** {@code SUM(dayPrice * coveredDays)} for an existing reservation from its bridge rows. */
    Optional<BigDecimal> sumReservationTotal(long reservationId);

    /** Quotes {@code SUM(dayPrice * coveredDays)} for {@code links} without persisting a reservation. */
    Optional<BigDecimal> quoteTotalFromLinks(List<ReservationAvailabilityLink> links);
}
