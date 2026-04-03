package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingAvailability;
import ar.edu.itba.paw.models.ListingSearchCriteria;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ListingService {

    Listing createListing(
            long carId,
            Listing.Status status,
            BigDecimal dayPrice,
            String startPoint,
            String description,
            List<AvailabilityPeriod> availabilityPeriods);

    Optional<Listing> getListingById(long id);

    List<ListingAvailability> findAvailabilityByListingId(long listingId);

    List<Listing> getAllListings();

    List<Listing> searchListings(ListingSearchCriteria criteria);

    List<Listing> getCheapestListings(int limit);

    List<Listing> getMostRecentListings(int limit);

    //limit cantidad maxima de listings que retorna
    List<Listing> findSimilarListings(long listingId, int limit);
}
