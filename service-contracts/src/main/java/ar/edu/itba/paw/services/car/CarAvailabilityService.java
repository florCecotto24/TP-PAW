package ar.edu.itba.paw.services.car;


import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.dto.car.AvailabilityCreateInput;
import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;

/**
 * Wall-calendar availability segments attached to a car ({@code car_availability} style persistence).
 * Dates are interpreted in the car wall zone when combined with reservation rules elsewhere; this contract only
 * moves rows in and out of storage. Callers own validation and transaction boundaries.
 * Implementations use {@code CarAvailabilityDao} only.
 */
public interface CarAvailabilityService {

    /** Availability row by id, when present. */
    Optional<CarAvailability> findById(long availabilityId);

    /**
     * Availability row scoped to a car: returns empty when the row does not exist or when it belongs
     * to a different car. Centralises the {@code av.getCarId() == carId} ownership check so callers
     * (controllers, view services) don't have to encode it themselves.
     */
    Optional<CarAvailability> findByIdForCar(long carId, long availabilityId);

    /**
     * The single availability row that is effective for {@code day} on a given car: the most recent row whose window
     * contains it. When the effective row is {@link CarAvailability.Kind#WITHDRAWN}, the day is not
     * bookable; otherwise its {@code dayPrice} is the effective price.
     */
    Optional<CarAvailability> findEffectiveForDayByCar(long carId, LocalDate day);

    /** All availability rows for {@code carId} whose window overlaps {@code [from, to]}, ordered by {@code createdAt} descending. */
    List<CarAvailability> findOverlappingRangeByCar(long carId, LocalDate from, LocalDate to);

    /** All availability segments for the car. */
    List<CarAvailability> findByCarId(long carId);

    /**
     * Paginated effective OFFERED rows for the whole car (no month filter).
     * Effectiveness is resolved in memory on the car's availability rows (bounded per car).
     */
    Page<CarAvailability> findEffectiveOfferedByCarPaginated(long carId, int page, int pageSize);

    /**
     * Paginated owner-calendar segments overlapping {@code [from, to]} (e.g. one calendar month).
     */
    Page<BookableSegmentProjection> getEffectiveSegmentsForOwnerCalendarInRangePaginated(
            long carId, LocalDate from, LocalDate to, int page, int pageSize);

    /** Batch load: rows for the given car ids whose {@code end_inclusive} is on or after {@code minEndDate}. */
    /**
     * Availability rows for the given car ids whose window reaches {@code minEndDate} or later.
     * When {@code minEndDate} is {@code null}, every row for those cars is returned.
     */
    List<CarAvailability> findByCarIdsEndingOnOrAfter(Collection<Long> carIds, LocalDate minEndDate);

    /**
     * Soft-deletes an availability period from the owner's calendar by inserting a brand new
     * {@link CarAvailability.Kind#WITHDRAWN} row covering the target's full window. The target row is
     * not mutated or deleted; the "most recent createdAt wins" resolution rule makes the days unbookable.
     * Aborts when blocking reservations intersect the target or when the target does not belong to {@code carId}.
     */
    CarAvailability applyOwnerWithdrawByCar(long carId, long availabilityId);

    /**
     * Soft-deletes a contiguous wall-day range by inserting a {@link CarAvailability.Kind#WITHDRAWN}
     * row for {@code [startInclusive, endInclusive]}. Used when the owner calendar shows merged
     * effective segments that may be narrower than a stored availability row.
     */
    void applyOwnerWithdrawRangeByCar(long carId, LocalDate startInclusive, LocalDate endInclusive);

    /**
     * Parses ISO wall dates and withdraws the inclusive range for the car owner calendar.
     *
     * @throws ar.edu.itba.paw.exception.car.CarValidationException when dates are missing or out of order
     */
    void parseAndApplyWithdrawRange(long carId, String fromInclusive, String untilInclusive);

    /**
     * Computes the bookable wall-day periods for every {@code carId} in a constant number of queries
     * (rather than one per car). Availability OFFERED rows are expanded to days and blocking
     * reservations are subtracted. Used by scheduler / batch flows.
     */
    Map<Long, List<AvailabilityPeriod>> getBookableWallAvailabilityPeriodsByCars(
            Collection<Long> carIds);

    /**
     * Batch variant of the rider date-picker check: {@code true} when
     * {@link #getBookableSegmentsForRiderDatePickerByCar(long, Instant)} would return a non-empty list.
     */
    Map<Long, Boolean> hasRiderBookableSegmentsByCarIds(Collection<Long> carIds, Instant now);

    /**
     * Returns the rider-facing bookable wall-day segments for the car, each carrying the effective per-day
     * attributes: {@code dayPrice}, {@code checkInTime}, {@code checkOutTime}, and a pre-formatted public
     * pickup location string. Each day's effective values come from the most recently created OFFERED
     * availability that covers that day. Contiguous days are merged into the same segment iff their full
     * projection is identical. The first day of each segment is clipped using its own {@code checkInTime}
     * and the rider pickup-lead policy relative to {@code now}.
     */
    List<BookableSegmentProjection> getBookableSegmentsForRiderDatePickerByCar(long carId, Instant now);

    /**
     * Atomic owner-side "publish availability" flow used by {@code MyCarsController.createListing}.
     * Performs every server-side gate in a single transaction (fail-fast):
     * <ol>
     *   <li>Reject when the owner of the car is currently blocked
     *       ({@link ar.edu.itba.paw.exception.MessageKeys#CAR_MUTATION_OWNER_BLOCKED}).</li>
     *   <li>Reject when the car's model is still pending validation
     *       ({@link ar.edu.itba.paw.exception.MessageKeys#CAR_CREATE_MODEL_PENDING}).</li>
     *   <li>Reject when the owner has no valid CBU yet
     *       ({@link ar.edu.itba.paw.exception.MessageKeys#CAR_PUBLISH_CBU_REQUIRED}).</li>
     *   <li>Re-validate the rider pickup lead time on every period start and the
     *       publish horizon against the wall calendar.</li>
     *   <li>Persist the availability rows and the car's {@code minimumRentalDays}.</li>
     * </ol>
     *
     * @param ownerUserId id of the signed-in user attempting the publication
     * @param carId       car primary key (ownership is enforced by the web filter chain on
     *                    {@code /my-cars/car/{carId}/**}; this method only checks owner-blocked
     *                    + CBU + model state)
     * @param input       form-derived availability payload
     * @param now         clock used by the rider-lead validation
     * @throws ar.edu.itba.paw.exception.car.CarValidationException                   on any of the
     *         publish-gate failures listed above
     * @throws ar.edu.itba.paw.exception.car.AvailabilityRiderLeadViolationException  when a
     *         period start violates the rider pickup lead time
     */
    List<CarAvailability> createListing(long ownerUserId, long carId, AvailabilityCreateInput input, Instant now);

    /**
     * Atomic owner-side edit of a single availability window used by
     * {@code MyCarsController.editAvailability}. Loads the availability row, enforces the
     * {@code availability.carId == carId} ownership cross-check, re-validates the rider
     * pickup lead (skipped when the start is unchanged) and the publish horizon against the
     * wall calendar, then inserts a new OFFERED row for the new window plus one WITHDRAWN
     * row for each removed-day chunk.
     *
     * @throws ar.edu.itba.paw.exception.car.CarValidationException                   when the
     *         availability does not exist, does not belong to {@code carId}, or any other gate fails
     * @throws ar.edu.itba.paw.exception.car.AvailabilityRiderLeadViolationException  when the
     *         new start violates the rider pickup lead (and changed vs. the original start)
     */
    CarAvailability editAvailability(long ownerUserId, long carId, long availabilityId, AvailabilityCreateInput input, Instant now);
}
