package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.HomeListingCards;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingCard;
import ar.edu.itba.paw.models.ListingDetail;
import ar.edu.itba.paw.models.ListingSearchCriteria;
import ar.edu.itba.paw.models.Page;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ListingDao {

    Listing createListing(
            long carId,
            String title,
            Listing.Status status,
            BigDecimal dayPrice,
            String startPoint,
            String description,
            LocalTime checkInTime,
            LocalTime checkOutTime);

    Optional<Listing> getListingById(long id);

    Optional<ListingDetail> getListingDetailById(long id);

    boolean updateOwnerListing(
            long ownerId,
            long listingId,
            BigDecimal dayPrice,
            String startPoint,
            String description,
            LocalTime checkInTime,
            LocalTime checkOutTime);

    boolean toggleListingStatus(long ownerId, long listingId);

    List<Listing> getAllListings();

    List<Listing> searchListings(ListingSearchCriteria criteria);

    List<Listing> getCheapestListings(int limit);

    List<Listing> getMostRecentListings(int limit);

    Page<ListingCard> getCheapestListingCards(int page, int pageSize);

    Page<ListingCard> getMostRecentListingCards(int page, int pageSize);

    Page<ListingCard> getOwnerListingCards(long ownerId, int page, int pageSize);

    boolean hasListingsByOwner(long ownerId);

    HomeListingCards getHomeListingCards(int limit);

    List<ListingCard> searchListingCards(ListingSearchCriteria criteria);

    List<ListingCard> findSimilarListingCards(long listingId, int limit);
}
