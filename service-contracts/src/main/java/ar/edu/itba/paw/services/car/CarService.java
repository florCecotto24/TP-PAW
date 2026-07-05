package ar.edu.itba.paw.services.car;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.paw.dto.GalleryMediaUpload;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.car.CarPriceMarketInsight;
import ar.edu.itba.paw.models.dto.car.ConsumerCarCardMarketContext;
import ar.edu.itba.paw.models.dto.car.OwnerCarDetailPageModel;
import ar.edu.itba.paw.models.util.search.CarSearchCriteria;
import ar.edu.itba.paw.models.util.search.CarSearchRequest;
import ar.edu.itba.paw.models.util.search.OwnerCarSearchCriteria;

/**
 * Car rows for owners and public catalog/search (browse cheapest/most-recent, search, owner hub,
 * counterparty profile grid, owner detail page).
 */
public interface CarService {

    /**
     * Sort argument for {@link #buildOwnerCarSearchCriteria} when loading the counterparty profile
     * "other active cars" grid (initial page and load-more): higher average rating first, then newer car.
     */
    String COUNTERPARTY_OTHER_ACTIVE_CARS_SORT = "rating,desc";

    /** Persists a car for {@code ownerId} linked to the given catalog model. {@code year} is optional. */
    Car createCar(
            long ownerId,
            String plate,
            long carModelId,
            Integer year,
            Car.Powertrain powertrain,
            Car.Transmission transmission);

    /**
     * Creates a car (linked to the given catalog model), saves its pictures, and optionally stores an
     * insurance document in one transaction (step 1 of the two-step publish flow). Duplicate plate
     * check is performed here. When {@code insuranceData} is {@code null}/empty the car is created in
     * {@link Car.Status#LACK_DOC}; otherwise it stays {@link Car.Status#ACTIVE}.
     * {@code description} is optional free-text (max 200 chars) and may be {@code null}.
     * {@code year} is optional manufacture year (1886 .. current year).
     */
    Car publishCar(
            long ownerId,
            String plate,
            long carModelId,
            Integer year,
            Car.Powertrain powertrain,
            Car.Transmission transmission,
            String description,
            List<GalleryMediaUpload> galleryMedia,
            String insuranceFilename,
            String insuranceContentType,
            byte[] insuranceData);

    /** Returns true if the owner already has a car registered with the given plate. */
    boolean existsByOwnerAndPlate(long ownerId, String plate);

    /** Loads a car by primary key when present. */
    Optional<Car> getCarById(final long id);

    /** All cars for the owner (with or without an active/paused listing), paginated and filtered. */
    Page<CarCard> getOwnerCarCards(OwnerCarSearchCriteria criteria);

    /**
     * Toggles a car between {@link Car.Status#ACTIVE} and {@link Car.Status#PAUSED}.
     * Validates ownership and CBU when activating.
     *
     * @return the new {@link Car.Status} after the toggle
     * @throws ar.edu.itba.paw.exception.car.CarValidationException when CBU is required or the
     *         car's current status does not allow toggling (e.g. {@code DEACTIVATED}, {@code ADMIN_PAUSED})
     */
    Car.Status toggleCarStatus(long ownerId, long carId);

    /**
     * Permanently deactivates a car (owner-initiated terminal state).
     *
     * @return {@code true} when the car existed and belonged to the owner
     */
    boolean deactivateCar(long ownerId, long carId);

    /**
     * Returns a list of at most {@code limit} active cars similar to the given car
     * (matching type, powertrain, and transmission) that are bookable on or after {@code browseWallDate},
     * excluding the reference car and optionally the owner's own cars.
     */
    List<CarCard> findSimilarCarCards(long carId, int limit, User viewer);

    /**
     * Public browse: active cars with at least one bookable wall day, ordered by ascending minimum
     * effective day price, paginated. Includes the viewer's own cars (owners can see how their
     * listings show up on the home page).
     */
    Page<CarCard> getCheapestCarCards(int page, int pageSize);

    /**
     * Public browse: active cars with at least one bookable wall day, ordered by most recently
     * published (latest listing creation), paginated. Includes the viewer's own cars (owners can
     * see how their listings show up on the home page).
     */
    Page<CarCard> getMostRecentCarCards(int page, int pageSize);

    /**
     * Public search: paginated car cards for the search results page. {@code criteria} carries
     * filters (text, body type, transmission, powertrain, price range, rating bands), the
     * requested availability window, and pagination/sort options.
     */
    Page<CarCard> searchCarCards(CarSearchCriteria criteria);

    /**
     * Builds {@link CarSearchCriteria} from raw home/search form parameters
     * (text, filters, wall dates or flexible month/days, pagination, sort) bundled into a
     * {@link CarSearchRequest} so the contract does not grow positional arguments.
     */
    CarSearchCriteria buildSearchCriteria(CarSearchRequest request);

    /**
     * Sweeps active cars that have no future bookable wall day and transitions them to
     * {@link Car.Status#PAUSED} so the wall stops surfacing exhausted vehicles.
     */
    void refreshExhaustedCarsToPaused();

    /**
     * Builds {@link OwnerCarSearchCriteria} from raw owner-hub form parameters
     * (filters, sort, pagination defaults).
     */
    OwnerCarSearchCriteria buildOwnerCarSearchCriteria(
            long ownerId,
            List<Car.Type> category,
            List<Car.Transmission> transmission,
            List<Car.Powertrain> powertrain,
            BigDecimal priceMin,
            BigDecimal priceMax,
            List<Car.Status> carStatus,
            List<String> rating,
            String textQuery,
            int page,
            int pageSize,
            String sort);

    /**
     * Same as {@link #buildOwnerCarSearchCriteria(long, List, List, List, BigDecimal, BigDecimal, List, List, String, int, int, String)}
     * but allows excluding a specific car id from the result.
     * Used by the counterparty profile "other active cars" grid.
     */
    OwnerCarSearchCriteria buildOwnerCarSearchCriteria(
            long ownerId,
            List<Car.Type> category,
            List<Car.Transmission> transmission,
            List<Car.Powertrain> powertrain,
            BigDecimal priceMin,
            BigDecimal priceMax,
            List<Car.Status> carStatus,
            List<String> rating,
            String textQuery,
            int page,
            int pageSize,
            String sort,
            Long excludeCarId);

    /**
     * Resolves which car statuses are visible for an owner-scoped listing ({@code GET /cars?ownerId=}):
     * the caller's own explicit filter when present (assumes the caller already checked the viewer is
     * allowed to filter by status), or {@code null} (no filter, i.e. every status) for the owner/admin
     * default view, or {@code ACTIVE}-only for any other viewer — browsing "cars owned by X" publicly
     * only shows currently-active listings, paused/deactivated/pending cars stay private to their owner.
     */
    List<Car.Status> resolveOwnerListingStatuses(List<Car.Status> requestedStatuses, boolean viewerIsSelfOrAdmin);

    /**
     * Market price stats (min / max / average) for active cars with the same brand and model.
     * {@code excludeCarId} omits one car when editing (typically the current one).
     */
    Optional<CarPriceMarketInsight> getPriceMarketInsightForCar(
            Car car, Long excludeCarId);

    /**
     * Market badge context per car id for consumer browse cards. Missing keys mean no badge for that card.
     */
    Map<Long, ConsumerCarCardMarketContext> resolveConsumerPriceMarketContexts(List<CarCard> cards);

    /**
     * Owner car detail page model assembled from the car, its availabilities, pictures, and reservation analytics.
     * Returns empty when the car cannot be found.
     */
    Optional<OwnerCarDetailPageModel> buildOwnerCarDetailPageModel(
            long carId, Locale locale);

    /**
     * For each car of the owner whose status is {@link Car.Status#ACTIVE} or {@link Car.Status#PAUSED},
     * transitions it to {@link Car.Status#LACK_DOC} and sends one email per affected car to the owner.
     */
    void pauseCarsForMissingCbu(long ownerId);

    /**
     * Re-activates cars that were paused only for missing CBU, after a valid CBU is stored.
     */
    void resumeCarsForRestoredCbu(long ownerId);

    /**
     * Stores an insurance document for the car (one slot per car). Re-uploading replaces any previous file.
     * When the car is in {@link Car.Status#LACK_DOC} solely because of the missing insurance, it transitions
     * back to {@link Car.Status#ACTIVE}.
     */
    void uploadValidatedCarInsuranceDocument(long ownerId, long carId, String originalFilename, String contentType, byte[] data);

    /** Clears the insurance file reference for the car, optionally moving the car back to {@link Car.Status#LACK_DOC}. */
    void clearCarInsuranceDocument(long ownerId, long carId);

    /** Returns true when the car has an insurance document on file. */
    boolean hasUploadedInsurance(long carId);

    /**
     * Returns {@code true} when the car's linked catalog model exists but has not yet been
     * validated by an admin (i.e. was created through the "Other" free-text path during publish).
     * Returns {@code false} when the car does not exist or its model is already validated.
     */
    boolean isModelPendingValidation(long carId);

    /** Updates the minimum rental days for a car via dirty-checking. */
    void updateMinimumRentalDays(long carId, int days);

    /** Persists the car's average rating via dirty-checking; {@code null} clears the value. */
    void updateRatingAvg(long carId, BigDecimal average);

    /** Updates optional free-text description for a car owned by {@code ownerId}. */
    void updateDescription(long ownerId, long carId, String description);

    // -----------------------------------------------------------------------------------------------------------
    // Admin-orchestrated operations on car rows.
    //
    // These methods exist so that {@link AdminService} can mutate car state without bypassing the layering rule
    // "each service may only call its own DAO". They are intentionally narrow: the calling {@link AdminService}
    // owns the surrounding admin policy (e.g. forbidding pausing an admin-owned car, cascading reservation
    // cancellations, sending notification emails).
    // -----------------------------------------------------------------------------------------------------------

    /** Admin-only: all cars currently in {@link Car.Status#ADMIN_PAUSED}. */
    List<Car> findAdminPausedCars();

    /** Admin-only: paginated list of all cars in the catalog. */
    Page<Car> findAllCarsPaginated(int page, int pageSize);

    /** All cars in the catalog linked to the given model id. Admin uses it to enumerate cars affected by a
     *  catalog validation/rejection. */
    List<Car> findCarsByModelId(long modelId);

    /** Admin-only: transitions the car to {@link Car.Status#ADMIN_PAUSED}. Throws when the car does not exist. */
    void markCarAsAdminPaused(long carId);

    /** Admin-only: transitions a car from {@link Car.Status#ADMIN_PAUSED} back to {@link Car.Status#ACTIVE}.
     *  Throws when the car is not currently admin-paused. */
    void releaseAdminCarPause(long carId);

    /** Admin-only: orphans the car from its catalog model (used when the catalog model is rejected and removed). */
    void clearCarModel(long carId);
}
