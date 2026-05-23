package ar.edu.itba.paw.services;

import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.util.OwnerListingSearchCriteria;

import java.util.List;
import java.util.Optional;

/**
 * Car rows for owners; catalog slices use {@code app.listing.car-catalog-limit} (not UI pagination).
 */
public interface CarService {

    /** Persists a car for {@code ownerId} linked to the given catalog model. */
    Car createCar(
            long ownerId,
            String plate,
            long carModelId,
            Car.Type type,
            Car.Powertrain powertrain,
            Car.Transmission transmission);

    /**
     * @deprecated Use {@link #createCar(long, String, long, Car.Type, Car.Powertrain, Car.Transmission)}.
     *             Resolves {@code brand}/{@code model} via {@link CarBrandService} / {@link CarModelService}
     *             internally; kept for the deprecated {@code ListingService#publish} flow until Fase 7.
     */
    @Deprecated
    Car createCar(
            long ownerId,
            String plate,
            String brand,
            String model,
            Car.Type type,
            Car.Powertrain powertrain,
            Car.Transmission transmission);

    /**
     * Creates a car (linked to the given catalog model) and saves its pictures in one transaction
     * (step 1 of the two-step publish flow). Duplicate plate check is performed here.
     */
    Car publishCar(
            long ownerId,
            String plate,
            long carModelId,
            Car.Type type,
            Car.Powertrain powertrain,
            Car.Transmission transmission,
            List<ImageUpload> images);

    /** Returns true if the owner already has a car registered with the given plate. */
    boolean existsByOwnerAndPlate(long ownerId, String plate);

    /** Loads a car by primary key when present. */
    Optional<Car> getCarById(final long id);

    /** Cars with an {@code active} status, ordered by ascending listing day price (row cap {@code app.listing.car-catalog-limit}). */
    List<Car> getCheapestCars();

    /** Cars with an {@code active} status, ordered by listing creation time descending (row cap {@code app.listing.car-catalog-limit}). */
    List<Car> getMostRecentCars();

    /** All cars for the owner (with or without an active/paused listing), paginated and filtered. */
    Page<CarCard> getOwnerCarCards(OwnerListingSearchCriteria criteria);

    /**
     * Toggles a car between {@link Car.Status#ACTIVE} and {@link Car.Status#PAUSED}.
     * Validates ownership and CBU when activating. Also syncs the most-recent listing status.
     *
     * @return the new {@link Car.Status} after the toggle
     * @throws ar.edu.itba.paw.exception.listing.ListingValidationException when CBU is required or the
     *         car's current status does not allow toggling (e.g. {@code DEACTIVATED}, {@code ADMIN_PAUSED})
     */
    Car.Status toggleCarStatus(long ownerId, long carId);

    /**
     * Permanently deactivates a car (owner-initiated terminal state).
     * The most-recent active/paused listing is finished as a side effect.
     *
     * @return {@code true} when the car existed and belonged to the owner
     */
    boolean deactivateCar(long ownerId, long carId);

    /**
     * Sets a car's status to {@link Car.Status#LACK_DOC} (e.g. missing CBU).
     * No-op if the car does not exist. Does not change the listing status directly
     * (that is handled by {@link ar.edu.itba.paw.services.ListingService#pauseActiveListingsDueToMissingCbuForOwnerAndNotify}).
     */
    void setCarLackDoc(long carId);

    /**
     * Clears a car's {@link Car.Status#LACK_DOC} status back to {@link Car.Status#ACTIVE}.
     * No-op if the car does not exist or is not in {@code LACK_DOC}.
     */
    void clearCarLackDoc(long carId);

    /**
     * Returns a list of at most {@code limit} active cars similar to the given car
     * (matching type, powertrain, and transmission) that are bookable on or after {@code browseWallDate},
     * excluding the reference car and optionally the owner's own cars.
     */
    List<CarCard> findSimilarCarCards(long carId, int limit, User viewer);

    /**
     * Public browse: active cars with at least one bookable wall day, ordered by ascending minimum
     * effective day price, paginated. Excludes the viewer's own cars when present.
     */
    Page<CarCard> getCheapestCarCards(int page, int pageSize, User viewer);

    /**
     * Public browse: active cars with at least one bookable wall day, ordered by most recently
     * published (latest listing creation), paginated. Excludes the viewer's own cars when present.
     */
    Page<CarCard> getMostRecentCarCards(int page, int pageSize, User viewer);

    /**
     * Public search: paginated car cards for the search results page. {@code criteria} carries
     * filters (text, body type, transmission, powertrain, price range, rating bands), the
     * requested availability window, and pagination/sort options.
     */
    Page<CarCard> searchCarCards(ar.edu.itba.paw.models.util.ListingSearchCriteria criteria);

    /**
     * Builds {@link ar.edu.itba.paw.models.util.ListingSearchCriteria} from raw home/search form
     * parameters (text, filters, wall dates, pagination, sort).
     */
    ar.edu.itba.paw.models.util.ListingSearchCriteria buildSearchCriteria(
            String query,
            java.util.List<String> category,
            java.util.List<String> transmission,
            java.util.List<String> powertrain,
            java.math.BigDecimal priceMin,
            java.math.BigDecimal priceMax,
            java.util.List<String> rating,
            String from,
            String until,
            int page,
            String sort,
            User viewer,
            java.util.List<Long> neighborhoodIds);

    /**
     * Sweeps active cars that have no future bookable wall day and transitions them to
     * {@link Car.Status#PAUSED} so the wall stops surfacing exhausted vehicles.
     */
    void refreshExhaustedCarsToFinished();

    /**
     * Builds {@link ar.edu.itba.paw.models.util.OwnerListingSearchCriteria} from raw owner-hub form parameters
     * (filters, sort, pagination defaults).
     */
    ar.edu.itba.paw.models.util.OwnerListingSearchCriteria buildOwnerCarSearchCriteria(
            long ownerId,
            java.util.List<String> category,
            java.util.List<String> transmission,
            java.util.List<String> powertrain,
            java.math.BigDecimal priceMin,
            java.math.BigDecimal priceMax,
            java.util.List<String> listingStatus,
            java.util.List<String> rating,
            String textQuery,
            int page,
            String sort);

    /**
     * For each active car of the owner whose status is {@link Car.Status#ACTIVE} or
     * {@link Car.Status#PAUSED}, transitions it to {@link Car.Status#LACK_DOC} and notifies the
     * owner by email (one mail per affected car).
     */
    void pauseActiveCarsDueToMissingCbuForOwnerAndNotify(long ownerId);

    /**
     * Re-activates cars that were paused only for missing CBU, after a valid CBU is stored.
     */
    void resumeCarsPausedDueToMissingCbuForOwner(long ownerId);
}
