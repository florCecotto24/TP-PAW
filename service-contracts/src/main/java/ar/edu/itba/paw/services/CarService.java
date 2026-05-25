package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.CarPriceMarketInsight;
import ar.edu.itba.paw.models.dto.ConsumerCarCardMarketContext;
import ar.edu.itba.paw.models.dto.OwnerCarDetailPageModel;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.util.CarSearchCriteria;
import ar.edu.itba.paw.models.util.OwnerCarSearchCriteria;

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

    /** Persists a car for {@code ownerId} linked to the given catalog model. */
    Car createCar(
            long ownerId,
            String plate,
            long carModelId,
            Car.Type type,
            Car.Powertrain powertrain,
            Car.Transmission transmission);

    /**
     * Creates a car (linked to the given catalog model), saves its pictures, and optionally stores an
     * insurance document in one transaction (step 1 of the two-step publish flow). Duplicate plate
     * check is performed here. When {@code insuranceData} is {@code null}/empty the car is created in
     * {@link Car.Status#LACK_DOC}; otherwise it stays {@link Car.Status#ACTIVE}.
     * {@code description} is optional free-text (max 200 chars) and may be {@code null}.
     */
    Car publishCar(
            long ownerId,
            String plate,
            long carModelId,
            Car.Type type,
            Car.Powertrain powertrain,
            Car.Transmission transmission,
            String description,
            List<ImageUpload> images,
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
    Page<CarCard> searchCarCards(CarSearchCriteria criteria);

    /**
     * Builds {@link CarSearchCriteria} from raw home/search form parameters
     * (text, filters, wall dates, pagination, sort).
     */
    CarSearchCriteria buildSearchCriteria(
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
            List<String> category,
            List<String> transmission,
            List<String> powertrain,
            BigDecimal priceMin,
            BigDecimal priceMax,
            List<String> carStatus,
            List<String> rating,
            String textQuery,
            int page,
            String sort);

    /**
     * Same as {@link #buildOwnerCarSearchCriteria(long, List, List, List, BigDecimal, BigDecimal, List, List, String, int, String)}
     * but allows overriding the page size and excluding a specific car id from the result.
     * Used by the counterparty profile "other active cars" grid.
     */
    OwnerCarSearchCriteria buildOwnerCarSearchCriteria(
            long ownerId,
            List<String> category,
            List<String> transmission,
            List<String> powertrain,
            BigDecimal priceMin,
            BigDecimal priceMax,
            List<String> carStatus,
            List<String> rating,
            String textQuery,
            int page,
            String sort,
            int pageSizeOrZero,
            Long excludeCarId);

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
}
