package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.HomeListingCards;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingAvailability;
import ar.edu.itba.paw.models.ListingCard;
import ar.edu.itba.paw.models.ListingDetail;
import ar.edu.itba.paw.models.ListingSearchCriteria;

public interface ListingService {

    Listing createListing(
            long carId,
            Listing.Status status,
            BigDecimal dayPrice,
            String startPoint,
            String description,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            List<AvailabilityPeriod> availabilityPeriods);

    Optional<Listing> getListingById(long id);

    Optional<ListingDetail> getListingDetailById(long id);

    List<ListingAvailability> findAvailabilityByListingId(long listingId);

    List<AvailabilityPeriod> getBookableWallAvailabilityPeriods(long listingId);

    boolean reservationIntervalFitsListingAvailability(
            long listingId,
            Long availabilityId,
            OffsetDateTime startDate,
            OffsetDateTime endDate);

    List<Listing> getAllListings();

    List<Listing> searchListings(ListingSearchCriteria criteria);

    List<Listing> getCheapestListings(int limit);

    List<Listing> getMostRecentListings(int limit);

    List<ListingCard> getCheapestListingCards(int limit);

    List<ListingCard> getMostRecentListingCards(int limit);

    HomeListingCards getHomeListingCards(int limit);

    List<ListingCard> searchListingCards(ListingSearchCriteria criteria);

    List<ListingCard> findSimilarListingCards(long listingId, int limit);

    ListingSearchCriteria buildSearchCriteria(
            String query,
            List<String> category,
            List<String> transmission,
            List<String> powertrain,
            List<String> price,
            String from,
            String until);
}
