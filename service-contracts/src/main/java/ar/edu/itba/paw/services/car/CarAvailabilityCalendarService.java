package ar.edu.itba.paw.services.car;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.CarAvailability;
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

    /** See {@link CarAvailabilityService#getBookableWallAvailabilityPeriodsForRiderDatePickerByCar(long, LocalTime, Instant)}. */
    List<AvailabilityPeriod> getBookableWallAvailabilityPeriodsForRiderDatePickerByCar(
            long carId, LocalTime checkInTime, Instant now);

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
}
