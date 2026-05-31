package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;
import ar.edu.itba.paw.models.util.time.AppTimezone;

/**
 * Wall-calendar availability segments attached to a car ({@code listing_availability} style persistence).
 * Dates are interpreted in the car wall zone when combined with reservation rules elsewhere; this contract only
 * moves rows in and out of storage. Callers own validation and transaction boundaries.
 * Implementations use {@code ListingAvailabilityDao} only.
 */
public interface ListingAvailabilityService {

    /** Availability row by id, when present. */
    Optional<ListingAvailability> findById(long availabilityId);

    /**
     * The single availability row that is effective for {@code day} on a given car: the most recent row whose window
     * contains it. When the effective row is {@link ListingAvailability.Kind#WITHDRAWN}, the day is not
     * bookable; otherwise its {@code dayPrice} is the effective price.
     */
    Optional<ListingAvailability> findEffectiveForDayByCar(long carId, LocalDate day);

    /** All availability rows for {@code carId} whose window overlaps {@code [from, to]}, ordered by {@code createdAt} descending. */
    List<ListingAvailability> findOverlappingRangeByCar(long carId, LocalDate from, LocalDate to);

    /**
     * Creates a full set of availability periods for a car without creating a {@code Listing} entity.
     * Each period gets a {@link ar.edu.itba.paw.models.domain.ListingAvailability.Kind#OFFERED} row
     * with the given price/location/time defaults. Per-period prices override the default when provided.
     * Also persists {@code minimumRentalDays} on the car after validating it does not exceed any period.
     *
     * @param carId             owning car primary key
     * @param dayPrice          default price per day (used for periods whose index has no entry in {@code periodPrices})
     * @param street            pickup street
     * @param number            pickup street number (nullable)
     * @param neighborhoodId    neighborhood id (nullable)
     * @param checkInTime       wall check-in time
     * @param checkOutTime      wall check-out time
     * @param periods           non-empty list of availability windows
     * @param periodPrices      per-period price overrides; may be shorter than {@code periods}
     * @param minimumRentalDays minimum consecutive days a rider must book (≥ 1)
     * @return the list of persisted offered rows
     */
    List<ListingAvailability> createCarAvailabilityPeriods(
            long carId,
            BigDecimal dayPrice,
            String street,
            String number,
            Long neighborhoodId,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            List<AvailabilityPeriod> periods,
            List<BigDecimal> periodPrices,
            int minimumRentalDays);

    /**
     * Updates the minimum rental days for an already-published car.
     * Validates that {@code minDays} does not exceed the shortest currently-effective offered period.
     *
     * @throws ar.edu.itba.paw.exception.car.CarValidationException when the new minimum exceeds a period
     */
    void updateMinimumRentalDays(long carId, int minDays);

    /** All availability segments for the car. */
    List<ListingAvailability> findByCarId(long carId);

    /**
     * Returns only the currently-effective {@link ListingAvailability.Kind#OFFERED} rows for the car —
     * those that still "win" at least one calendar day under the "most recent {@code createdAt} wins" rule.
     * Superseded rows (fully covered by newer rows of any kind) are excluded, as are
     * {@link ListingAvailability.Kind#WITHDRAWN} rows (internal bookkeeping).
     * Intended for the owner's availability list view.
     */
    List<ListingAvailability> findEffectiveOfferedByCar(long carId);

    /** Batch load: rows for the given car ids whose {@code end_inclusive} is on or after {@code minEndDate}. */
    List<ListingAvailability> findByCarIdsEndingOnOrAfter(Collection<Long> carIds, LocalDate minEndDate);

    /** Removes every availability row for {@code carId} (e.g. before replacing the whole wall). */
    void deleteByCarId(long carId);

    /**
     * Applies an owner edit to an availability window on a car. Mirrors {@link #applyOwnerWithdrawByCar}
     * but for an edit: inserts a new {@link ListingAvailability.Kind#OFFERED} row and one
     * {@link ListingAvailability.Kind#WITHDRAWN} row per removed-day chunk.
     */
    ListingAvailability applyOwnerEditByCar(
            long carId,
            LocalDate oldStartInclusive,
            LocalDate oldEndInclusive,
            LocalDate newStartInclusive,
            LocalDate newEndInclusive,
            BigDecimal dayPrice,
            String startPointStreet,
            String startPointNumber,
            Long neighborhoodId,
            LocalTime checkInTime,
            LocalTime checkOutTime);

    /**
     * Soft-deletes an availability period from the owner's calendar by inserting a brand new
     * {@link ListingAvailability.Kind#WITHDRAWN} row covering the target's full window. The target row is
     * not mutated or deleted; the "most recent createdAt wins" resolution rule makes the days unbookable.
     * Aborts when blocking reservations intersect the target or when the target does not belong to {@code carId}.
     */
    ListingAvailability applyOwnerWithdrawByCar(long carId, long availabilityId);

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
     * Returns the minimum effective day price across all future offered availability rows for the car.
     * When {@code defaultPrice} is provided it seeds the minimum (used as a fallback when no availability rows exist).
     */
    BigDecimal resolveMinEffectiveDayPriceByCar(long carId, BigDecimal defaultPrice);

    /**
     * Returns {@code true} when not all future offered availability rows have the same price as {@code defaultPrice}.
     */
    boolean isCarPriceVariableByCar(long carId, BigDecimal defaultPrice);

    /**
     * Returns the most recent {@link ListingAvailability} row for the car (highest {@code createdAt}).
     * Useful to read the owner's last-published location and check-in/out times when {@code Listing} is gone.
     */
    Optional<ListingAvailability> findMostRecentByCarId(long carId);

    /**
     * First wall-calendar day allowed for a new availability "from" field, taking the configured
     * rider pickup lead time and the {@code checkInTime} into account.
     */
    LocalDate getPublicationMinAvailabilityFirstWallDay(LocalTime checkInTime, Instant now);

    /**
     * Publication rule: each period start must be on or after {@link #getPublicationMinAvailabilityFirstWallDay}.
     *
     * @throws ar.edu.itba.paw.exception.car.AvailabilityRiderLeadViolationException on the first violating row
     */
    void validatePublicationAvailabilityRiderLead(
            List<AvailabilityPeriod> periods, LocalTime checkInTime, Instant now);

    /**
     * Like {@link #validatePublicationAvailabilityRiderLead} but for edits: the lead-time
     * check on the start date is skipped when the new start equals {@code originalStartInclusive},
     * because the period may already be in progress and the lead time was satisfied at creation.
     *
     * @throws ar.edu.itba.paw.exception.car.AvailabilityRiderLeadViolationException when the start
     *         date changed and the new start violates the rider pickup lead
     */
    void validateEditAvailabilityRiderLead(
            List<AvailabilityPeriod> periods,
            LocalTime checkInTime,
            Instant now,
            LocalDate originalStartInclusive);

    /**
     * Publication rule: availability dates must lie within {@link #getConfiguredMaxAvailabilityForwardWallDays()}
     * of "today" in {@link AppTimezone#WALL_ZONE}.
     */
    void validatePublicationAvailabilityAgainstWallCalendar(List<AvailabilityPeriod> periods);

    /**
     * Max inclusive wall-calendar end date offset from {@code LocalDate.now(AppTimezone.WALL_ZONE)} for
     * published availability ({@code app.listing.max-availability-forward-wall-days}).
     */
    int getConfiguredMaxAvailabilityForwardWallDays();
}
