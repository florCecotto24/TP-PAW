package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ListingAvailability;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ListingAvailabilityDao {

    ListingAvailability create(long listingId, LocalDate startInclusive, LocalDate endInclusive);

    List<ListingAvailability> findByListingId(long listingId);

    Optional<ListingAvailability> findActiveById(long id);

    void setActive(long id, boolean active);

    void updateDateRange(long id, LocalDate startInclusive, LocalDate endInclusive);
}
