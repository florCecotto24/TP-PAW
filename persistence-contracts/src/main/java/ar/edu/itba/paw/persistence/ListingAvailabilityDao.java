package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ListingAvailability;

import java.time.OffsetDateTime;
import java.util.List;

public interface ListingAvailabilityDao {

    ListingAvailability create(long listingId, OffsetDateTime startDate, OffsetDateTime endDate);

    List<ListingAvailability> findByListingId(long listingId);
}
