package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.dto.CarPublicationResult;
import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.HomeListingCards;
import ar.edu.itba.paw.models.dto.ListingCard;
import ar.edu.itba.paw.models.dto.ListingDetail;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.util.ListingSearchCriteria;
import ar.edu.itba.paw.models.util.OwnerListingSearchCriteria;

/**
 * Listing catalog, wall availability, search/browse, and publish flows.
 * The implementation uses {@code ListingDao} only; cars, availability segments, and blocking reservations go through
 * {@code CarService}, {@code ListingAvailabilityService}, and {@code ReservationService}.
 */
public interface ListingService {

    /**
     * Persists a new listing row for an existing car (price, pickup address, check-in/out, availability, neighborhood).
     */
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
     * Publishes a car with listing, availability windows, and images (full publish flow).
     * Runs in a single transaction: if any step fails after inserts begin, the whole operation rolls back
     * (no orphan car or listing). The owner must have a valid CBU before any row is written; otherwise
     * {@link ar.edu.itba.paw.exception.listing.ListingValidationException} is thrown immediately.
     * The web UI blocks submit until CBU is saved ({@code /publish-car/quick-cbu}); cancelling the modal
     * means no POST to this flow, so nothing is persisted.
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

    /** Loads a listing by primary key, when present. */
    Optional<Listing> getListingById(long id);

    /** Public detail projection (car, media, reviews slice) for the listing detail page. */
    Optional<ListingDetail> getListingDetailById(long id);

    /**
     * Updates an owned listing’s core fields and replaces availability rows.
     *
     * @return {@code true} when the listing existed, belonged to {@code ownerId}, and was updated
     */
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

    /**
     * Toggles active/inactive for an owned listing when business rules allow.
     *
     * @return {@code true} when the transition was applied
     */
    boolean toggleListingStatus(long ownerId, long listingId);

    /**
     * Marks an owned listing as finished (owner-initiated terminal state when allowed).
     *
     * @return {@code true} when the listing was updated
     */
    boolean finishListing(long ownerId, long listingId);

    /**
     * If the listing is active and has no bookable wall day from today onward, sets status to paused.
     */
    void refreshListingFinishedIfExhausted(long listingId);

    /**
     * Pauses every active listing without future bookable days (batch; for scheduled sweeps).
     */
    void refreshExhaustedListingsToFinished();

    /** Raw availability rows for the listing (DB order). */
    List<ListingAvailability> findAvailabilityByListingId(long listingId);

    /** Merged wall-calendar periods that still accept new reservations for this listing. */
    List<AvailabilityPeriod> getBookableWallAvailabilityPeriods(long listingId);

    /**
     * Bookable wall days for the listing date picker: same as {@link #getBookableWallAvailabilityPeriods(long)} then
     * merged and clipped so the first selectable pickup respects configured rider pickup lead from {@code now}.
     */
    List<AvailabilityPeriod> getBookableWallAvailabilityPeriodsForRiderDatePicker(
            long listingId, LocalTime listingPickupWall, Instant now);

    /**
     * First wall-calendar day allowed for a new listing's availability "from" fields (pickup lead vs {@code now}).
     */
    LocalDate getPublicationMinAvailabilityFirstWallDay(LocalTime listingPickupWall, Instant now);

    /**
     * Publication: each period start must be on or after {@link #getPublicationMinAvailabilityFirstWallDay}.
     *
     * @throws ar.edu.itba.paw.exception.listing.AvailabilityRiderLeadViolationException on the first violating row
     */
    void validatePublicationAvailabilityRiderLead(List<AvailabilityPeriod> periods, LocalTime checkInTime, Instant now);

    /**
     * Whether {@code [startDate, endDate]} lies entirely inside published availability (optional concrete
     * availability row when {@code availabilityId} is non-null).
     */
    boolean reservationIntervalFitsListingAvailability(
            long listingId,
            Long availabilityId,
            OffsetDateTime startDate,
            OffsetDateTime endDate);

    /** All listings (admin-style listing). */
    List<Listing> getAllListings();

    /** Filtered listing rows for back-office or legacy search. */
    List<Listing> searchListings(ListingSearchCriteria criteria);

    /** Up to {@code limit} lowest-priced active listings. */
    List<Listing> getCheapestListings(int limit);

    /** Up to {@code limit} most recently created active listings. */
    List<Listing> getMostRecentListings(int limit);

    /** Paginated cheapest cards for home-style grids; {@code viewer} affects personalized fields when applicable. */
    Page<ListingCard> getCheapestListingCards(int page, int pageSize, User viewer);

    /** Paginated most-recent cards for home-style grids. */
    Page<ListingCard> getMostRecentListingCards(int page, int pageSize, User viewer);

    /** Owner dashboard: paginated cards for one owner with filters and sort. */
    Page<ListingCard> getOwnerListingCards(OwnerListingSearchCriteria criteria);

    /**
     * {@code sort} argument for {@link #buildOwnerListingSearchCriteria} when loading the counterparty profile
     * “other active listings” grid (initial page and load-more): higher average rating first, then newer listing.
     */
    String COUNTERPARTY_OTHER_ACTIVE_LISTINGS_SORT = "rating,desc";

    /**
     * Builds {@link OwnerListingSearchCriteria} from request parameters (sanitizes enums, sort, pagination defaults).
     */
    OwnerListingSearchCriteria buildOwnerListingSearchCriteria(
            long ownerId,
            List<String> category,
            List<String> transmission,
            List<String> powertrain,
            BigDecimal priceMin,
            BigDecimal priceMax,
            List<String> listingStatus,
            List<String> rating,
            String textQuery,
            int page,
            String sort);

    /**
     * Same as {@link #buildOwnerListingSearchCriteria(long, List, List, List, BigDecimal, BigDecimal, List, List, String, int, String)}
     * with explicit page size (when {@code pageSizeOrZero} &gt; 0) and optional listing id to exclude from SQL results.
     */
    OwnerListingSearchCriteria buildOwnerListingSearchCriteria(
            long ownerId,
            List<String> category,
            List<String> transmission,
            List<String> powertrain,
            BigDecimal priceMin,
            BigDecimal priceMax,
            List<String> listingStatus,
            List<String> rating,
            String textQuery,
            int page,
            String sort,
            int pageSizeOrZero,
            Long excludeListingId);

    /** Whether the owner has at least one listing row. */
    boolean hasListingsByOwner(long ownerId);

    /** Home page: mixed cheapest/recent slices up to {@code limit} cards. */
    HomeListingCards getHomeListingCards(int limit, User viewer);

    /** Public search: paginated cards from {@link ListingSearchCriteria}. */
    Page<ListingCard> searchListingCards(ListingSearchCriteria criteria);

    /**
     * Similar active listings for the detail page: same body type, powertrain, and transmission as the reference
     * car, excluding the current listing (and optionally the viewer’s own listings when authenticated).
     */
    List<ListingCard> findSimilarListingCards(long listingId, int limit, User viewer);

    /**
     * Builds {@link ListingSearchCriteria} from home/search form parameters (text, filters, wall dates, pagination).
     */
    ListingSearchCriteria buildSearchCriteria(
            String query,
            List<String> category,
            List<String> transmission,
            List<String> powertrain,
            BigDecimal priceMin,
            BigDecimal priceMax,
            List<String> rating,
            String from,
            String until,
            int page,
            String sort,
            User viewer,
            List<Long> neighborhoodIds);

    /**
     * Max inclusive wall-calendar end date offset from {@code LocalDate.now(AvailabilityPeriod.WALL_ZONE)} for
     * published listing availability ({@code app.listing.max-availability-forward-wall-days}).
     */
    int getConfiguredMaxAvailabilityForwardWallDays();

    /** {@code app.reservation.pickup-lead-hours} (same source as {@link ReservationService#getConfiguredPickupLeadHours()}). */
    int getConfiguredPickupLeadHours();

    /**
     * Publication rule: availability dates must lie within {@link #getConfiguredMaxAvailabilityForwardWallDays()}
     * of “today” in {@link AvailabilityPeriod#WALL_ZONE}.
     */
    void validatePublicationAvailabilityAgainstWallCalendar(List<AvailabilityPeriod> periods);

    /**
     * For each active listing of the owner, sets status to {@link Listing.Status#PAUSED_DUE_TO_LACK_OF_CBU} and
     * notifies the owner by email (one mail per affected listing).
     */
    void pauseActiveListingsDueToMissingCbuForOwnerAndNotify(long ownerId);

    /**
     * Re-activates listings that were paused only for missing CBU, after a valid CBU is stored.
     */
    void resumeListingsPausedDueToMissingCbuForOwner(long ownerId);
}
