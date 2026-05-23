package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.dto.HomeListingCards;
import ar.edu.itba.paw.models.dto.ListingCard;
import ar.edu.itba.paw.models.dto.ListingDetail;
import ar.edu.itba.paw.models.dto.ListingPriceMarketInsight;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.util.ListingSearchCriteria;
import ar.edu.itba.paw.models.util.OwnerListingSearchCriteria;

/** Listings, search/browse projections, and home-card queries. */
public interface ListingDao {

    Listing createListing(
            long carId,
            String title,
            Listing.Status status,
            BigDecimal dayPrice,
            String startPointStreet,
            String startPointNumber,
            String description,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            Long neighborhoodId);

    Optional<Listing> getListingById(long id);

    /**
     * {@code check_in_time} per listing (pickup lead-time rules in search / browse).
     */
    Map<Long, LocalTime> findCheckInTimeByListingIds(Collection<Long> listingIds);

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
            Long neighborhoodId);

    boolean toggleListingStatus(long ownerId, long listingId);

    /**
     * Sets {@code newStatus} only if the row's current status is one of {@code allowedFrom}.
     *
     * @return whether a row was updated
     */
    boolean updateListingStatus(long listingId, Listing.Status newStatus, Listing.Status... allowedFrom);

    /**
     * Listings whose status is one of the given values, ordered by id (e.g. batch maintenance sweeps).
     */
    List<Listing> findListingsWithStatuses(Listing.Status... statuses);

    List<Listing> getAllListings();

    List<Listing> searchListings(ListingSearchCriteria criteria);

    List<Listing> getCheapestListings(int limit);

    List<Listing> getMostRecentListings(int limit);

    /**
     * Listings for the car owner in the given status (e.g. enforce CBU / resume after CBU).
     */
    List<Listing> findListingsByOwnerIdAndStatus(long ownerId, Listing.Status status);

    /**
     * Ordered window for public browse (cheapest first). Caller composes UI pagination with {@link #countBrowseEligibleActiveListings}.
     */
    List<ListingCard> getCheapestListingCardsWindow(int offset, int limit, LocalDate browseWallDate, Long excludeOwnerUserId);

    /**
     * Ordered window for public browse (newest first). Caller composes UI pagination with {@link #countBrowseEligibleActiveListings}.
     */
    List<ListingCard> getMostRecentListingCardsWindow(int offset, int limit, LocalDate browseWallDate, Long excludeOwnerUserId);

    Page<ListingCard> getOwnerListingCards(OwnerListingSearchCriteria criteria);

    /** Returns the most recent non-finished listing for the given car, if any. */
    Optional<Listing> findActiveOrPausedListingByCar(long carId);

    /** Returns the most recent listing for the given car regardless of status, if any. */
    Optional<Listing> findMostRecentListingByCar(long carId);

    boolean hasListingsByOwner(long ownerId);

    HomeListingCards getHomeListingCards(int limit, LocalDate browseWallDate, Long excludeOwnerUserId);

    /**
     * Search listing cards with database-backed pagination when the criteria do not include
     * a wall-calendar availability range (that path still filters in memory after SQL).
     */
    Page<ListingCard> searchListingCards(ListingSearchCriteria criteria);

    List<ListingCard> findSimilarListingCards(long listingId, int limit, LocalDate browseWallDate, Long excludeOwnerUserId);

    long countBrowseEligibleActiveListings(LocalDate browseWallDate, Long excludeOwnerUserId);

    /**
     * Min, max, and average {@code day_price} of active listings whose car matches {@code brand} and
     * {@code model} (case-insensitive, trimmed). Optionally excludes one listing (e.g. the one being edited).
     */
    Optional<ListingPriceMarketInsight> findActiveDayPriceMarketInsightByBrandAndModel(
            String brand, String model, Long excludeListingId);
}
