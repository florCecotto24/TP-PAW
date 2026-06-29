package ar.edu.itba.paw.services.car;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;

/**
 * Bookable-day calendar derivations + per-car pricing aggregates extracted out of the
 * monolithic availability service. Implementations are read-only and joined to the
 * caller's transaction when one is active.
 *
 * <p>The public {@link CarAvailabilityService} interface keeps these methods declared as well
 * for backward compatibility with existing callers; its implementation simply delegates each
 * call to this service. New code SHOULD depend on this contract directly.</p>
 */
public interface CarAvailabilityCalendarService {

    /** See {@link CarAvailabilityService#getBookableWallAvailabilityPeriodsByCar(long)}. */
    List<AvailabilityPeriod> getBookableWallAvailabilityPeriodsByCar(long carId);

    /**
     * Batch variant of {@link #getBookableWallAvailabilityPeriodsByCar(long)}: returns the
     * merged bookable wall-day periods for each {@code carId}, fetching every car's offered
     * availability + blocking reservations in a single round trip rather than {@code N} per-car
     * queries. Cars with no bookable periods map to an empty list. Used by scheduler / batch
     * flows that need the same calculation across many cars at once.
     */
    Map<Long, List<AvailabilityPeriod>> getBookableWallAvailabilityPeriodsByCars(Collection<Long> carIds);

    /** See {@link CarAvailabilityService#getBookableWallAvailabilityPeriodsForRiderDatePickerByCar(long, LocalTime, Instant)}. */
    List<AvailabilityPeriod> getBookableWallAvailabilityPeriodsForRiderDatePickerByCar(
            long carId, LocalTime checkInTime, Instant now);

    /**
     * Batch variant of the rider date-picker check: {@code true} when
     * {@link #getBookableSegmentsForRiderDatePickerByCar(long, Instant)} would return a non-empty list
     * (pickup lead + blocking reservations applied).
     */
    Map<Long, Boolean> hasRiderBookableSegmentsByCarIds(Collection<Long> carIds, Instant now);

    /** See {@link CarAvailabilityService#getBookableSegmentsForRiderDatePickerByCar(long, Instant)}. */
    List<BookableSegmentProjection> getBookableSegmentsForRiderDatePickerByCar(long carId, Instant now);

    /** See {@link CarAvailabilityService#getBookableSegmentsForRiderDatePickerByCarExcluding(long, Instant, long)}. */
    List<BookableSegmentProjection> getBookableSegmentsForRiderDatePickerByCarExcluding(
            long carId, Instant now, long excludingReservationId);

    /** See {@link CarAvailabilityService#getAllEffectiveSegmentsForOwnerCalendar(long)}. */
    List<BookableSegmentProjection> getAllEffectiveSegmentsForOwnerCalendar(long carId);

    /** See {@link CarAvailabilityService#findEffectiveOfferedByCar(long)}. */
    List<CarAvailability> findEffectiveOfferedByCar(long carId);

    /** See {@link CarAvailabilityService#resolveMinEffectiveDayPriceByCar(long, BigDecimal)}. */
    BigDecimal resolveMinEffectiveDayPriceByCar(long carId, BigDecimal defaultPrice);

    /** See {@link CarAvailabilityService#isCarPriceVariableByCar(long, BigDecimal)}. */
    boolean isCarPriceVariableByCar(long carId, BigDecimal defaultPrice);

    /**
     * Like {@link #findEffectiveOfferedByCar(long)} but scoped to rows overlapping
     * {@code [from, to]}. Only rows that overlap the given range are loaded from the DB,
     * and effectiveness is computed on that subset.
     */
    List<CarAvailability> findEffectiveOfferedByCarInRange(long carId, LocalDate from, LocalDate to);

    /**
     * Like {@link #getAllEffectiveSegmentsForOwnerCalendar(long)} but scoped to rows overlapping
     * {@code [from, to]}. Used for the owner calendar to load a ±1 month window.
     */
    List<BookableSegmentProjection> getEffectiveSegmentsForOwnerCalendarInRange(long carId, LocalDate from, LocalDate to);
}
