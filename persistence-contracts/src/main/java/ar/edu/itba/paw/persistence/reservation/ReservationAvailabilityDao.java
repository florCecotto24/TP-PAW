package ar.edu.itba.paw.persistence.reservation;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.car.CarAvailability;

/**
 * N:N bridge rows between reservations and the {@code car_availability} rows that were considered
 * when pricing the reservation. The per-day winning availability is resolved at read time by date
 * range and {@code MAX(created_at)}; no per-row date segmentation is persisted.
 *
 * <p>Access is restricted to
 * {@link ar.edu.itba.paw.services.reservation.ReservationAvailabilityServiceImpl}.</p>
 */
public interface ReservationAvailabilityDao {

    /**
     * Persists the bridge rows linking {@code reservationId} to each given {@code availabilityIds}.
     * Call within the same transaction as
     * {@link ReservationDao#createReservationForCar}. Duplicate ids are de-duplicated.
     */
    void insertCoveringAvailabilities(long reservationId, Collection<Long> availabilityIds);

    /**
     * Removes every bridge row attached to {@code reservationId}. Used by the rider-side edit flow
     * before re-inserting the freshly-computed covering availabilities for the new period.
     */
    void deleteCoveringAvailabilities(long reservationId);

    /**
     * Reconstructs the reservation total by, for each wall-calendar day of the reservation, picking
     * among the bridged availabilities the one whose range covers the day with the most recent
     * {@code created_at}, and summing its {@code day_price}.
     *
     * @return total when every day of the reservation is covered; empty otherwise (or when the
     *         reservation does not exist).
     */
    Optional<BigDecimal> sumReservationTotal(long reservationId);

    /**
     * Returns the snapshot {@link CarAvailability} that drives pickup display (address, check-in /
     * check-out times) for the reservation: among the bridged candidates that {@code OFFERED}-cover
     * the reservation's first wall-calendar day, the one with the latest {@code created_at} wins
     * (ties broken by id). Anchors the rider-visible pickup info to the rows captured at pricing
     * time, so later owner edits that insert new availability rows on the same date range do not
     * mutate the address / times shown for already-bridged reservations.
     *
     * @return the winning {@code CarAvailability}; empty when the reservation has no bridge rows or
     *         when none of the bridged candidates is an OFFERED row covering the first day.
     */
    Optional<CarAvailability> findEffectivePickupAvailabilityForReservation(long reservationId);
}
