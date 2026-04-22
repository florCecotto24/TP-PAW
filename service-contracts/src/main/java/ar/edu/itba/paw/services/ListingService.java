package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.dto.CarPublicationResult;
import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.HomeListingCards;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingAvailability;
import ar.edu.itba.paw.models.ListingCard;
import ar.edu.itba.paw.models.ListingDetail;
import ar.edu.itba.paw.models.ListingSearchCriteria;
import ar.edu.itba.paw.models.Page;
import ar.edu.itba.paw.models.User;

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

    /**
     * Publica un auto con aviso, disponibilidad e imágenes (flujo de publicación completo).
     */
    CarPublicationResult publish(
            long ownerId,
            String plate,
            String brand,
            String model,
            Car.Type type,
            Car.Powertrain powertrain,
            Car.Transmission transmission,
            BigDecimal pricePerDay,
            String startPoint,
            String description,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            List<AvailabilityPeriod> periods,
            List<ImageUpload> images);

    Optional<Listing> getListingById(long id);

    Optional<ListingDetail> getListingDetailById(long id);

    boolean updateOwnerListing(
            long ownerId,
            long listingId,
            BigDecimal dayPrice,
            String startPoint,
            String description,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            List<AvailabilityPeriod> availabilityPeriods);

    boolean toggleListingStatus(long ownerId, long listingId);

    /**
     * If the listing is active or paused and has no bookable wall day from today onward, sets status to finished.
     */
    void refreshListingFinishedIfExhausted(long listingId);

    /**
     * Marks every active or paused listing without future bookable days as finished (batch; for scheduled sweeps).
     */
    void refreshExhaustedListingsToFinished();

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

    Page<ListingCard> getCheapestListingCards(int page, int pageSize, User viewer);

    Page<ListingCard> getMostRecentListingCards(int page, int pageSize, User viewer);

    Page<ListingCard> getOwnerListingCards(long ownerId, int page, int pageSize);

    boolean hasListingsByOwner(long ownerId);

    HomeListingCards getHomeListingCards(int limit, User viewer);

    Page<ListingCard> searchListingCards(ListingSearchCriteria criteria);

    List<ListingCard> findSimilarListingCards(long listingId, int limit, User viewer);

    ListingSearchCriteria buildSearchCriteria(
            String query,
            List<String> category,
            List<String> transmission,
            List<String> powertrain,
            List<String> price,
            String from,
            String until,
            int page,
            String sort,
            User viewer);
}
