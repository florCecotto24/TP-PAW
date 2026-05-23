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

/**
 * Wall-calendar availability segments attached to a listing ({@code listing_availability} style persistence).
 * Dates are interpreted in the listing wall zone when combined with reservation rules elsewhere; this contract only
 * moves rows in and out of storage. Callers such as {@code ListingService} own validation and transaction boundaries.
 * Implementations use {@code ListingAvailabilityDao} only.
 */
public interface ListingAvailabilityService {

    /**
     * Inserts one contiguous availability window for {@code listingId} with an optional per-period price.
     *
     * @param listingId        owning listing primary key
     * @param startInclusive   first bookable wall-calendar day (inclusive)
     * @param endInclusive     last bookable wall-calendar day (inclusive)
     * @param dayPrice         optional price per day for this period; {@code null} means use the listing-level price
     * @return persisted row including generated id and timestamps
     */
    ListingAvailability create(long listingId, LocalDate startInclusive, LocalDate endInclusive, BigDecimal dayPrice);

    default ListingAvailability create(long listingId, LocalDate startInclusive, LocalDate endInclusive) {
        return create(listingId, startInclusive, endInclusive, null);
    }

    /**
     * All segments for {@code listingId}, typically ordered by start date ascending in the JDBC implementation.
     */
    List<ListingAvailability> findByListingId(long listingId);

    /** Availability row by id, when present. */
    Optional<ListingAvailability> findById(long availabilityId);

    /**
     * Batch load for several listings: rows whose {@code end_inclusive} is on or after {@code minEndDate}
     * (used when pruning past days while keeping future availability).
     *
     * @param listingIds non-null collection of listing ids (may be empty; behavior is DAO-defined, usually empty list)
     * @param minEndDate lower bound on segment end (wall-local calendar)
     */
    List<ListingAvailability> findByListingIdsEndingOnOrAfter(Collection<Long> listingIds, LocalDate minEndDate);

    /** Removes every availability row for {@code listingId} (e.g. before replacing the whole wall). */
    void deleteByListingId(long listingId);

    /**
     * The single availability row that is effective for {@code day}: the most recent row whose window
     * contains it. When the effective row is {@link ListingAvailability.Kind#WITHDRAWN}, the day is not
     * bookable; otherwise its {@code dayPrice} is the effective price.
     */
    Optional<ListingAvailability> findEffectiveForDay(long listingId, LocalDate day);

    /**
     * All availability rows for {@code listingId} whose window overlaps {@code [from, to]}, ordered by
     * {@code createdAt} descending so callers can resolve the per-day winner in memory.
     */
    List<ListingAvailability> findOverlappingRange(long listingId, LocalDate from, LocalDate to);

    // ---- Car-id based variants (Phase 7b+) ----

    /** Like {@link #findEffectiveForDay(long, LocalDate)} but resolves by {@code car_id}. */
    Optional<ListingAvailability> findEffectiveForDayByCar(long carId, LocalDate day);

    /** Like {@link #findOverlappingRange(long, LocalDate, LocalDate)} but resolves by {@code car_id}. */
    List<ListingAvailability> findOverlappingRangeByCar(long carId, LocalDate from, LocalDate to);

    /**
     * Creates a full set of availability periods for a car without creating a {@code Listing} entity.
     * Each period gets a {@link ar.edu.itba.paw.models.domain.ListingAvailability.Kind#OFFERED} row
     * with the given price/location/time defaults. Per-period prices override the default when provided.
     *
     * @param carId         owning car primary key
     * @param dayPrice      default price per day (used for periods whose index has no entry in {@code periodPrices})
     * @param street        pickup street
     * @param number        pickup street number (nullable)
     * @param neighborhoodId neighborhood id (nullable)
     * @param checkInTime   wall check-in time
     * @param checkOutTime  wall check-out time
     * @param periods       non-empty list of availability windows
     * @param periodPrices  per-period price overrides; may be shorter than {@code periods}
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
            List<BigDecimal> periodPrices);

    /** Like {@link #findByListingId} but resolves by {@code car_id}. */
    List<ListingAvailability> findByCarId(long carId);

    /**
     * Like {@link #findByListingIdsEndingOnOrAfter} but resolves by {@code car_id}.
     */
    List<ListingAvailability> findByCarIdsEndingOnOrAfter(Collection<Long> carIds, LocalDate minEndDate);

    /** Like {@link #deleteByListingId} but resolves by {@code car_id}. */
    void deleteByCarId(long carId);

    /**
     * Like {@link #applyOwnerEdit} but operates on a car directly (no listing required).
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
     * Like {@link #applyOwnerWithdrawAvailability} but verifies ownership by {@code car_id}
     * instead of {@code listing_id}.
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
     * Returns the minimum effective day price across all future offered availability rows for the car.
     * When {@code defaultPrice} is provided it seeds the minimum (used as a fallback when no availability rows exist).
     */
    BigDecimal resolveMinEffectiveDayPriceByCar(long carId, BigDecimal defaultPrice);

    /**
     * Returns {@code true} when not all future offered availability rows have the same price as {@code defaultPrice}.
     */
    boolean isCarPriceVariableByCar(long carId, BigDecimal defaultPrice);

    /**
     * Applies an owner edit to an availability window. The operation does NOT mutate or delete the
     * existing row: it inserts a new {@link ListingAvailability.Kind#OFFERED} row for
     * {@code [newStartInclusive, newEndInclusive]} and one
     * {@link ListingAvailability.Kind#WITHDRAWN} row per contiguous chunk of days that are in the old
     * window but not in the new one ({@code old \ new}). Because the "most recent createdAt wins"
     * resolution rule already handles overlaps, this leaves a clean per-day result on the calendar.
     * If any of the days that get withdrawn have an active reservation
     * ({@code pending}/{@code accepted}/{@code started}), the operation aborts by throwing
     * {@link ar.edu.itba.paw.exception.reservation.ReservationConflictException} with message key
     * {@code listing.availability.editConflict}.
     *
     * @param listingId           owning listing
     * @param oldStartInclusive   start of the previously published window (used to compute removed days)
     * @param oldEndInclusive     end of the previously published window
     * @param newStartInclusive   start of the new window
     * @param newEndInclusive     end of the new window
     * @param dayPrice            price for the new offered window
     * @param startPointStreet    pickup street for the new offered window
     * @param startPointNumber    pickup number for the new offered window (nullable)
     * @param neighborhoodId      neighborhood id for the new offered window (nullable)
     * @param checkInTime         wall check-in for the new offered window
     * @param checkOutTime        wall check-out for the new offered window
     * @return the newly persisted offered row (the withdrawn rows are persisted too but not returned)
     */
    ListingAvailability applyOwnerEdit(
            long listingId,
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
     * {@link ListingAvailability.Kind#WITHDRAWN} row covering the target's full {@code [startInclusive,
     * endInclusive]} window, using the target's own pricing/location/time as template values. The
     * target row itself is NOT mutated nor deleted; the "most recent createdAt wins" resolution rule
     * makes the days unbookable from this point on.
     * <p>The operation aborts with {@link ar.edu.itba.paw.exception.reservation.ReservationConflictException}
     * ({@code listing.availability.withdrawConflict}) when any blocking reservation
     * ({@code pending}/{@code accepted}/{@code started}) intersects the target window.
     * It also rejects (via {@link ar.edu.itba.paw.exception.listing.ListingValidationException})
     * when the target does not exist, does not belong to {@code listingId}, or is itself already
     * {@code withdrawn}.
     *
     * @param listingId       expected owning listing of {@code availabilityId} (ownership check)
     * @param availabilityId  the availability period the owner wants to remove from the UI
     * @return the newly persisted withdrawn row
     */
    ListingAvailability applyOwnerWithdrawAvailability(long listingId, long availabilityId);

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
     * @throws ar.edu.itba.paw.exception.listing.AvailabilityRiderLeadViolationException on the first violating row
     */
    void validatePublicationAvailabilityRiderLead(
            List<AvailabilityPeriod> periods, LocalTime checkInTime, Instant now);

    /**
     * Publication rule: availability dates must lie within {@link #getConfiguredMaxAvailabilityForwardWallDays()}
     * of "today" in {@link AvailabilityPeriod#WALL_ZONE}.
     */
    void validatePublicationAvailabilityAgainstWallCalendar(List<AvailabilityPeriod> periods);

    /**
     * Max inclusive wall-calendar end date offset from {@code LocalDate.now(AvailabilityPeriod.WALL_ZONE)} for
     * published availability ({@code app.listing.max-availability-forward-wall-days}).
     */
    int getConfiguredMaxAvailabilityForwardWallDays();
}
