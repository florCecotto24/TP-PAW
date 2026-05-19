package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.domain.ListingAvailability;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/** Wall-calendar availability segments for listings. */
public interface ListingAvailabilityDao {

    ListingAvailability create(long listingId, LocalDate startInclusive, LocalDate endInclusive, BigDecimal dayPrice);

    default ListingAvailability create(long listingId, LocalDate startInclusive, LocalDate endInclusive) {
        return create(listingId, startInclusive, endInclusive, null);
    }

    List<ListingAvailability> findByListingId(long listingId);

    /**
     * Availability rows for the given listings whose window reaches {@code minEndDate} or later.
     */
    List<ListingAvailability> findByListingIdsEndingOnOrAfter(Collection<Long> listingIds, LocalDate minEndDate);

    void deleteByListingId(long listingId);
}
