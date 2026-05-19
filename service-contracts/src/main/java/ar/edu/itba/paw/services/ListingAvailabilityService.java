package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

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
}
