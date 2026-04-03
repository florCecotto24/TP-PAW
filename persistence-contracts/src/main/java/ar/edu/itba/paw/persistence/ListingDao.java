package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingCard;
import ar.edu.itba.paw.models.ListingDetail;
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

    Optional<ListingDetail> getListingDetailById(long id);

    List<Listing> getAllListings();

    List<Listing> searchListings(ListingSearchCriteria criteria);

    List<Listing> getCheapestListings(int limit);

    List<Listing> getMostRecentListings(int limit);

    List<ListingCard> getCheapestListingCards(int limit);

    List<ListingCard> getMostRecentListingCards(int limit);

    List<ListingCard> searchListingCards(ListingSearchCriteria criteria);

    List<Listing> findSimilarListings(
            long excludedListingId,
            Car.Type type,
            Car.Powertrain powertrain,
            Car.Transmission transmission,
            int limit);
}
