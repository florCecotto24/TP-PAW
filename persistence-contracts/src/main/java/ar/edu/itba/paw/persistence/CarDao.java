package ar.edu.itba.paw.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.CarPriceMarketInsight;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.util.CarSearchCriteria;
import ar.edu.itba.paw.models.util.OwnerCarSearchCriteria;

/** JPA-backed access to {@code cars} and catalogue queries joined to active listings. */
public interface CarDao {
    /** Creates a car linked to the given catalog model. The {@code model_id} FK is set; legacy
     *  {@code brand}/{@code model} columns are no longer written. {@code year} is optional. */
    Car createCar(long ownerId, String plate, long carModelId, Integer year,
                  Car.Powertrain powertrain, Car.Transmission transmission);

    boolean existsByOwnerAndPlate(long ownerId, String plate);

    Optional<Car> getCarById(final long id);

    /** All cars for the owner (LEFT JOIN to non-finished listings), with the same filters as the owner listing grid. */
    Page<CarCard> getOwnerCarCards(OwnerCarSearchCriteria criteria);

    /** Updates a car's lifecycle status via dirty-checking (load + mutate). */
    void setCarStatus(long carId, Car.Status newStatus);

    /** Attaches an insurance file to the car (load + mutate). The file is assumed already valid. */
    void updateInsuranceDocument(long carId, long insuranceFileId);

    /** Detaches the insurance file from the car (sets FK to null). */
    void clearInsuranceDocument(long carId);

    /**
     * Returns at most {@code limit} active cars with the same type/powertrain/transmission as the reference car,
     * excluding the reference car and optionally the owner.
     */
    List<CarCard> findSimilarCarCards(long carId, int limit, LocalDate browseWallDate, Long excludeOwnerUserId);

    /** All cars for the owner whose status is one of {@code statuses}. */
    List<Car> findCarsByOwnerAndStatuses(long ownerId, java.util.Collection<Car.Status> statuses);

    /** All cars whose status equals {@code status}. */
    List<Car> findCarsByStatus(Car.Status status);

    /** All cars whose catalog model id equals {@code modelId}. */
    List<Car> findCarsByModelId(long modelId);

    /**
     * Atomically transitions a car from {@code expected} to {@code newStatus} only when its current
     * status matches {@code expected}. Returns {@code true} when the row was updated.
     */
    boolean updateCarStatusIfCurrent(long carId, Car.Status newStatus, Car.Status expected);

    /** Public browse cheapest cars window. Caller composes UI pagination with {@link #countBrowseEligibleActiveCars}. */
    List<CarCard> getCheapestCarCardsWindow(int offset, int limit, LocalDate browseWallDate, Long excludeOwnerUserId);

    /** Public browse most-recent cars window. */
    List<CarCard> getMostRecentCarCardsWindow(int offset, int limit, LocalDate browseWallDate, Long excludeOwnerUserId);

    /** Counts active cars with at least one offered availability ending on or after {@code browseWallDate}. */
    long countBrowseEligibleActiveCars(LocalDate browseWallDate, Long excludeOwnerUserId);

    /** Public car-card search across {@code cars} joined with {@code listing_availability}. */
    Page<CarCard> searchCarCards(CarSearchCriteria criteria);

    /**
     * Min, max, average and count of per-car minimum {@code day_price} values from active cars with
     * validated catalog brand/model and at least one {@code listing_availability} row (kind = 'offered').
     * Each car contributes one price ({@code MIN(day_price)} across its offered segments), matching browse-card
     * pricing. {@code excludeCarId} omits one car when editing (typically the current one).
     */
    Optional<CarPriceMarketInsight> findActiveDayPriceMarketInsightByBrandAndModel(
            String brand, String model, Long excludeCarId);

    /** Updates the minimum rental days for a car via dirty-checking (load + mutate). */
    void updateMinimumRentalDays(long carId, int days);
}
