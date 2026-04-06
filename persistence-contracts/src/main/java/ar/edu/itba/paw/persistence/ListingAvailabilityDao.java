package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ListingAvailability;

import java.time.LocalDate;
import java.util.List;

public interface ListingAvailabilityDao {

    ListingAvailability create(long listingId, LocalDate startInclusive, LocalDate endInclusive);

    List<ListingAvailability> findByListingId(long listingId);
}
