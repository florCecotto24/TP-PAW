package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.domain.CarAvailability;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Wall-calendar availability segments attached to a car. */
public interface CarAvailabilityDao {

    /** Single availability row by id, when present. */
    Optional<CarAvailability> findById(long availabilityId);

    /**
     * The single availability row that is effective for {@code day} on a car: the one whose
     * {@code [start_date, end_date]} contains {@code day} with the most recent {@code createdAt}
     * (ties broken by id desc). When the effective row has {@link CarAvailability.Kind#WITHDRAWN},
     * the day is not bookable; otherwise its {@code dayPrice} is the effective price.
     *
     * @return the effective row, or empty when no row covers {@code day}.
     */
    Optional<CarAvailability> findEffectiveForDayByCar(long carId, LocalDate day);

    /**
     * All availability rows for {@code carId} whose window overlaps {@code [from, to]}. The caller is
     * expected to resolve the "winner" per day in memory by ordering by {@code createdAt} desc.
     */
    List<CarAvailability> findOverlappingRangeByCar(long carId, LocalDate from, LocalDate to);

    /**
     * Inserts a fully-specified row bound to a {@code car_id} only. Used by the car-centric publish/edit
     * flow which needs to lay down both {@code OFFERED} and {@code WITHDRAWN} rows with explicit
     * per-row pricing/location/time settings.
     */
    CarAvailability createFullForCar(
            long carId,
            LocalDate startInclusive,
            LocalDate endInclusive,
            BigDecimal dayPrice,
            String startPointStreet,
            String startPointNumber,
            Long neighborhoodId,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            CarAvailability.Kind kind);

    /** All availability segments for the car (typically ordered by start date ascending). */
    List<CarAvailability> findByCarId(long carId);

    /** Availability rows for the given car ids whose window reaches {@code minEndDate} or later. */
    List<CarAvailability> findByCarIdsEndingOnOrAfter(Collection<Long> carIds, LocalDate minEndDate);

    /** Removes every availability row for {@code carId} (e.g. before replacing the whole wall). */
    void deleteByCarId(long carId);

    /**
     * Minimum {@link CarAvailability.Kind#OFFERED OFFERED} day price for each given car. Cars
     * without any offered availability are absent from the returned map. Useful for "from" pricing
     * displays.
     */
    Map<Long, BigDecimal> findMinOfferedDayPriceByCarIds(Collection<Long> carIds);
}
