package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingSearchCriteria;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ListingDao {

    Listing createListing(
            long carId,
            String title,
            Listing.Status status,
            BigDecimal dayPrice,
            String startPoint,
            String description);

    Optional<Listing> getListingById(long id);

    List<Listing> getAllListings();

    List<Listing> searchListings(ListingSearchCriteria criteria);
}
