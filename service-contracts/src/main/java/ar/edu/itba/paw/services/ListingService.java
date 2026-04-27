package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.dto.CarPublicationResult;
import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.dto.HomeListingCards;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.dto.ListingCard;
import ar.edu.itba.paw.models.dto.ListingDetail;
import ar.edu.itba.paw.models.util.ListingSearchCriteria;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;

public interface ListingService {

    Listing createListing(
            long carId,
            Listing.Status status,
            BigDecimal dayPrice,
            String startPointStreet,
            String startPointNumber,
            String description,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            List<AvailabilityPeriod> availabilityPeriods,
            Long neighborhoodId);

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
            String startPointStreet,
            String startPointNumber,
            String description,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            List<AvailabilityPeriod> periods,
            List<ImageUpload> images,
            Long neighborhoodId);

    Optional<Listing> getListingById(long id);

    Optional<ListingDetail> getListingDetailById(long id);

    boolean updateOwnerListing(
            long ownerId,
            long listingId,
            BigDecimal dayPrice,
            String startPointStreet,
            String startPointNumber,
            String description,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            List<AvailabilityPeriod> availabilityPeriods,
            Long neighborhoodId);

    boolean toggleListingStatus(long ownerId, long listingId);

        /**
         * Misma dirección que el retiro (calle + barrio sin altura), para vistas públicas.
         */
    String formatPublicDeliveryLocation(Listing listing);

    /**
     * Street + neighborhood of pickup (without number), for public views.
     */
    String formatPublicPickupLocation(Listing listing);

    /**
     * Same as {@link #formatFullPickupLocation(Listing)} (pickup and return in the same address).
     */
    String formatFullDeliveryLocation(Listing listing);

    String formatFullPickupLocation(Listing listing);

    /**
     * Same as {@link #formatFullPickupLocation(Listing)} (pickup and return in the same address).
     */
    String formatDeliveryForReservationView(Listing listing, Reservation reservation, boolean viewerIsOwner);

    /**
     * Pickup (and return); door number according to role and payment proof.
     */
    String formatPickupForReservationView(Listing listing, Reservation reservation, boolean viewerIsOwner);

    /**
     * Text for emails to the rider (without door number until payment proof).
     */
    String formatRiderReservationHandoverSummary(Listing listing, Reservation reservation);

    /**
     * Unique text for emails to the owner (full data).
     */
    String formatOwnerReservationHandoverSummary(Listing listing);

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

    Page<ListingCard> getOwnerListingCards(long ownerId, int page, int pageSize, String listingStatus, String textQuery);

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
            User viewer,
            List<Long> neighborhoodIds);

    /**
     * Max total inclusive days across all availability periods for a listing
     * ({@code app.listing.max-availability-total-days}).
     */
    int getConfiguredMaxAvailabilityTotalDays();
}
