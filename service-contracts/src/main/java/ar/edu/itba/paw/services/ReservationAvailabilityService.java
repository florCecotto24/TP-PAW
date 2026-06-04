package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

/**
 * N:N bridge between reservations and the {@code car_availability} rows considered when pricing
 * them. The per-day winning availability is resolved at read time by date range and
 * {@code MAX(created_at)}; no per-row date segmentation is persisted.
 */
public interface ReservationAvailabilityService {

    /**
     * Persists the bridge rows linking {@code reservationId} to each {@code availabilityIds}; joins
     * the caller's transaction when one is active. Duplicate ids are de-duplicated.
     */
    void insertCoveringAvailabilities(long reservationId, Collection<Long> availabilityIds);

    /**
     * Removes every bridge row attached to {@code reservationId}; joins the caller's transaction
     * when one is active. Used by the rider-side reservation edit flow to clear the old covering
     * availabilities before re-inserting the freshly computed ones.
     */
    void deleteCoveringAvailabilities(long reservationId);

    /**
     * Reconstructs the reservation total from the bridge rows by, for each wall-calendar day of the
     * reservation, picking among the bridged availabilities the one whose range covers the day with
     * the most recent {@code createdAt}, and summing its {@code dayPrice}.
     */
    Optional<BigDecimal> sumReservationTotal(long reservationId);
}
