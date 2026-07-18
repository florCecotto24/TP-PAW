package ar.edu.itba.paw.services.car;


import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;

/**
 * Bookable-day calendar derivations extracted out of the monolithic availability service.
 * Implementations are read-only and joined to the caller's transaction when one is active.
 */
public interface CarAvailabilityCalendarService {

    /**
     * Returns the merged bookable wall-day periods for each {@code carId}, fetching every car's
     * offered availability + blocking reservations in a single round trip rather than {@code N}
     * per-car queries. Cars with no bookable periods map to an empty list.
     */
    Map<Long, List<AvailabilityPeriod>> getBookableWallAvailabilityPeriodsByCars(Collection<Long> carIds);

    /**
     * Batch variant of the rider date-picker check: {@code true} when
     * {@link #getBookableSegmentsForRiderDatePickerByCar(long, Instant)} would return a non-empty list
     * (pickup lead + blocking reservations applied).
     */
    Map<Long, Boolean> hasRiderBookableSegmentsByCarIds(Collection<Long> carIds, Instant now);

    /** Rider-facing bookable wall-day segments for the car (pickup lead + blocking applied). */
    List<BookableSegmentProjection> getBookableSegmentsForRiderDatePickerByCar(long carId, Instant now);

    /**
     * Currently-effective {@link CarAvailability.Kind#OFFERED} rows for the car
     * (most recent {@code createdAt} wins per day).
     */
    List<CarAvailability> findEffectiveOfferedByCar(long carId);

    /**
     * Owner-calendar segments overlapping {@code [from, to]} (no reservation clipping).
     */
    List<BookableSegmentProjection> getEffectiveSegmentsForOwnerCalendarInRange(
            long carId, LocalDate from, LocalDate to);
}
