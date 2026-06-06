package ar.edu.itba.paw.services.car;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.CarAvailability;
import ar.edu.itba.paw.models.dto.car.AvailabilityCreateInput;
import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;
import ar.edu.itba.paw.models.util.time.AppTimezone;

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
     * The single availability row that is effective for {@code day} on a given car: the most recent row whose window
     * contains it. When the effective row is {@link CarAvailability.Kind#WITHDRAWN}, the day is not
     * bookable; otherwise its {@code dayPrice} is the effective price.
     */
    Optional<CarAvailability> findEffectiveForDayByCar(long carId, LocalDate day);

    /** All availability rows for {@code carId} whose window overlaps {@code [from, to]}, ordered by {@code createdAt} descending. */
    List<CarAvailability> findOverlappingRangeByCar(long carId, LocalDate from, LocalDate to);

    /**
     * Updates the minimum rental days for an already-published car.
     * Validates that {@code minDays} does not exceed the shortest currently-effective offered period.
     *
     * @throws ar.edu.itba.paw.exception.car.CarValidationException when the new minimum exceeds a period
     */
    void updateMinimumRentalDays(long carId, int minDays);

    /** All availability segments for the car. */
    List<CarAvailability> findByCarId(long carId);

    /**
     * Returns only the currently-effective {@link CarAvailability.Kind#OFFERED} rows for the car —
     * those that still "win" at least one calendar day under the "most recent {@code createdAt} wins" rule.
     * Superseded rows (fully covered by newer rows of any kind) are excluded, as are
     * {@link CarAvailability.Kind#WITHDRAWN} rows (internal bookkeeping).
     * Intended for the owner's availability list view.
     */
    List<CarAvailability> findEffectiveOfferedByCar(long carId);

    /** Batch load: rows for the given car ids whose {@code end_inclusive} is on or after {@code minEndDate}. */
    List<CarAvailability> findByCarIdsEndingOnOrAfter(Collection<Long> carIds, LocalDate minEndDate);

    /**
     * Wall-calendar day ranges {@code [startInclusive, endInclusive]} held by active blocking
     * reservations on the car (pending / accepted / started, where the rider currently has a
     * claim on the days). Used by the owner publish flow to (a) disable those days in the
     * Flatpickr picker, and (b) reject publish payloads that overlap them server-side.
     * Reservations that already ended in the wall-zone past are excluded.
     */
    List<AvailabilityPeriod> findReservationBlockedWallRangesByCar(long carId);

    /** Removes every availability row for {@code carId} (e.g. before replacing the whole wall). */
    void deleteByCarId(long carId);

    /**
     * Soft-deletes an availability period from the owner's calendar by inserting a brand new
     * {@link CarAvailability.Kind#WITHDRAWN} row covering the target's full window. The target row is
     * not mutated or deleted; the "most recent createdAt wins" resolution rule makes the days unbookable.
     * Aborts when blocking reservations intersect the target or when the target does not belong to {@code carId}.
     */
    CarAvailability applyOwnerWithdrawByCar(long carId, long availabilityId);

    /**
     * Computes the set of bookable wall-calendar days for the car.
     * Availability OFFERED rows are expanded to days; blocking reservations are subtracted.
     * The result is a compressed list of contiguous {@link AvailabilityPeriod} ranges.
     */
    List<AvailabilityPeriod> getBookableWallAvailabilityPeriodsByCar(long carId);

    /**
     * Like {@link #getBookableWallAvailabilityPeriodsByCar} but also clips the result based on the rider pickup
     * lead time, returning only days reachable by a rider.
     */
    List<AvailabilityPeriod> getBookableWallAvailabilityPeriodsForRiderDatePickerByCar(
            long carId,
            LocalTime checkInTime,
            Instant now);

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
     * Same as {@link #getBookableSegmentsForRiderDatePickerByCar(long, Instant)} but excludes the
     * reservation whose id matches {@code excludingReservationId} from the "blocking reservations"
     * subtraction. Used by the rider-side edit flow so the days currently held by the reservation under
     * edit are still considered bookable for that same reservation (rider can shrink/extend without
     * conflicting against themselves).
     */
    List<BookableSegmentProjection> getBookableSegmentsForRiderDatePickerByCarExcluding(
            long carId, Instant now, long excludingReservationId);

    /**
     * Returns the minimum effective day price across all future offered availability rows for the car.
     * When {@code defaultPrice} is provided it seeds the minimum (used as a fallback when no availability rows exist).
     */
    BigDecimal resolveMinEffectiveDayPriceByCar(long carId, BigDecimal defaultPrice);

    /**
     * Returns {@code true} when not all future offered availability rows have the same price as {@code defaultPrice}.
     */
    boolean isCarPriceVariableByCar(long carId, BigDecimal defaultPrice);

    /**
     * Returns the most recent {@link CarAvailability} row for the car (highest {@code createdAt}).
     * Useful to read the owner's last-published location and check-in/out times when {@code Listing} is gone.
     */
    Optional<CarAvailability> findMostRecentByCarId(long carId);

    /**
     * First wall-calendar day allowed for a new availability "from" field, taking the configured
     * rider pickup lead time and the {@code checkInTime} into account.
     */
    LocalDate getPublicationMinAvailabilityFirstWallDay(LocalTime checkInTime, Instant now);

    /**
     * Max inclusive wall-calendar end date offset from {@code LocalDate.now(AppTimezone.WALL_ZONE)} for
     * published availability ({@code app.listing.max-availability-forward-wall-days}).
     */
    int getConfiguredMaxAvailabilityForwardWallDays();

    /**
     * Returns all effective offered periods as {@link BookableSegmentProjection} without any
     * time-based clipping. Each {@link CarAvailability} row is mapped directly to one segment
     * (no adjacent-day merging). Intended for the owner-facing calendar that must show past
     * and future availability together.
     */
    List<BookableSegmentProjection> getAllEffectiveSegmentsForOwnerCalendar(long carId);

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
    List<CarAvailability> createListing(long ownerUserId, long carId, AvailabilityCreateInput input, java.time.Instant now);

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
    CarAvailability editAvailability(long ownerUserId, long carId, long availabilityId, AvailabilityCreateInput input, java.time.Instant now);

    /**
     * Atomic owner-side bulk replacement of all availability windows for a car used by
     * {@code MyCarsController.editCar}. When the car has no existing rows, this delegates to
     * {@link #createListing}. Otherwise it iterates the {@code input.periods()} list and edits
     * each window in place, using the most-recent existing row as the "old" window and
     * {@link AvailabilityCreateInput#effectivePriceAt(int)} to resolve the per-period price.
     * Owner-blocked / CBU / model-pending guards are inherited from the underlying services.
     * {@code minimumRentalDays = 1} is used when falling through to {@code createListing}.
     */
    void applyOwnerBulkEdit(long ownerUserId, long carId, AvailabilityCreateInput input, java.time.Instant now);
}
