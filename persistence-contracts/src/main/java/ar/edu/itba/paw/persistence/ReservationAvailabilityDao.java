package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

/**
 * N:N bridge rows between reservations and the {@code car_availability} rows that were considered
 * when pricing the reservation. The per-day winning availability is resolved at read time by date
 * range and {@code MAX(created_at)}; no per-row date segmentation is persisted.
 *
 * <p>Access is restricted to
 * {@link ar.edu.itba.paw.services.ReservationAvailabilityServiceImpl}.</p>
 */
public interface ReservationAvailabilityDao {

    /**
     * Persists the bridge rows linking {@code reservationId} to each given {@code availabilityIds}.
     * Call within the same transaction as
     * {@link ReservationDao#createReservationForCar}. Duplicate ids are de-duplicated.
     */
    void insertCoveringAvailabilities(long reservationId, Collection<Long> availabilityIds);

    /**
     * Reconstructs the reservation total by, for each wall-calendar day of the reservation, picking
     * among the bridged availabilities the one whose range covers the day with the most recent
     * {@code created_at}, and summing its {@code day_price}.
     *
     * @return total when every day of the reservation is covered; empty otherwise (or when the
     *         reservation does not exist).
     */
    Optional<BigDecimal> sumReservationTotal(long reservationId);
}
