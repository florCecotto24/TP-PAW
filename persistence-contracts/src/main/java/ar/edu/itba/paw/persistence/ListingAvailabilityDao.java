package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.domain.ListingAvailability;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Wall-calendar availability segments for listings. */
public interface ListingAvailabilityDao {

    ListingAvailability create(long listingId, LocalDate startInclusive, LocalDate endInclusive, BigDecimal dayPrice);

    default ListingAvailability create(long listingId, LocalDate startInclusive, LocalDate endInclusive) {
        return create(listingId, startInclusive, endInclusive, null);
    }

    /**
     * Inserts a fully-specified row (no defaults copied from the owning listing). Used by the owner
     * availability-edit flow which needs to lay down both {@code OFFERED} and {@code WITHDRAWN} rows
     * with explicit per-row pricing/location/time settings.
     */
    ListingAvailability createFull(
            long listingId,
            LocalDate startInclusive,
            LocalDate endInclusive,
            BigDecimal dayPrice,
            String startPointStreet,
            String startPointNumber,
            Long neighborhoodId,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            ListingAvailability.Kind kind);

    /** Single availability row by id, when present. */
    Optional<ListingAvailability> findById(long availabilityId);

    List<ListingAvailability> findByListingId(long listingId);

    /**
     * Availability rows for the given listings whose window reaches {@code minEndDate} or later.
     */
    List<ListingAvailability> findByListingIdsEndingOnOrAfter(Collection<Long> listingIds, LocalDate minEndDate);

    void deleteByListingId(long listingId);

    /**
     * The single availability row that is effective for {@code day}: the one whose
     * {@code [start_date, end_date]} contains {@code day} with the most recent {@code createdAt}
     * (ties broken by id desc). When the effective row has {@link ListingAvailability.Kind#WITHDRAWN},
     * the day is not bookable; otherwise its {@code dayPrice} is the effective price.
     *
     * @return the effective row, or empty when no row covers {@code day}.
     */
    Optional<ListingAvailability> findEffectiveForDay(long listingId, LocalDate day);

    /**
     * All availability rows for {@code listingId} whose window overlaps {@code [from, to]}. The caller is
     * expected to resolve the "winner" per day in memory by ordering by {@code createdAt} desc. Used by
     * the reservation flow to walk the wall calendar and persist the day-by-day breakdown into the
     * {@code reservations_availabilities} bridge table.
     */
    List<ListingAvailability> findOverlappingRange(long listingId, LocalDate from, LocalDate to);

    // ---- Car-id based variants (Phase 7b+: reservations no longer require listing_id) ----

    /**
     * Like {@link #findEffectiveForDay(long, LocalDate)} but resolves by {@code car_id} instead of
     * {@code listing_id}. Required once availability rows are inserted without a listing reference.
     */
    Optional<ListingAvailability> findEffectiveForDayByCar(long carId, LocalDate day);

    /**
     * Like {@link #findOverlappingRange(long, LocalDate, LocalDate)} but resolves by {@code car_id}.
     */
    List<ListingAvailability> findOverlappingRangeByCar(long carId, LocalDate from, LocalDate to);

    /**
     * Like {@link #createFull} but binds only to a {@code car_id} — no listing reference is set.
     * Used for new availability rows created in the car-centric publish flow (Phase 7c+).
     */
    ListingAvailability createFullForCar(
            long carId,
            LocalDate startInclusive,
            LocalDate endInclusive,
            BigDecimal dayPrice,
            String startPointStreet,
            String startPointNumber,
            Long neighborhoodId,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            ListingAvailability.Kind kind);

    /** Like {@link #findByListingId} but resolves by {@code car_id}. */
    List<ListingAvailability> findByCarId(long carId);

    /**
     * Like {@link #findByListingIdsEndingOnOrAfter} but resolves by {@code car_id}.
     */
    List<ListingAvailability> findByCarIdsEndingOnOrAfter(Collection<Long> carIds, LocalDate minEndDate);

    /** Like {@link #deleteByListingId} but resolves by {@code car_id}. */
    void deleteByCarId(long carId);
}
