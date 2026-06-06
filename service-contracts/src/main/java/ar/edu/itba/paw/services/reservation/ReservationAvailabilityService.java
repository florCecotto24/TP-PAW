package ar.edu.itba.paw.services.reservation;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.CarAvailability;

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

    /**
     * Returns the snapshot {@link CarAvailability} that drives pickup display (address,
     * check-in / check-out times) for the reservation. Among the bridged candidates that
     * {@code OFFERED}-cover the reservation's first wall-calendar day, the one with the latest
     * {@code createdAt} wins. Anchors the rider-visible pickup info to the rows captured at
     * pricing time, so later owner edits that insert new availability rows on the same date range
     * do not mutate the address / times shown for already-bridged reservations.
     */
    Optional<CarAvailability> findEffectivePickupAvailabilityForReservation(long reservationId);
}
