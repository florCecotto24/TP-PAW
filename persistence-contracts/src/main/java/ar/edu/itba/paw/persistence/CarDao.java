package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.util.OwnerListingSearchCriteria;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** JPA-backed access to {@code cars} and catalogue queries joined to active listings. */
public interface CarDao {
    /** Creates a car linked to the given catalog model. The {@code model_id} FK is set; legacy
     *  {@code brand}/{@code model} columns are no longer written. */
    Car createCar(long ownerId, String plate, long carModelId,
                  Car.Type type, Car.Powertrain powertrain, Car.Transmission transmission);

    boolean existsByOwnerAndPlate(long ownerId, String plate);

    Optional<Car> getCarById(final long id);

    /** Active listings ordered by ascending day price; row count capped by {@code app.listing.car-catalog-limit}. */
    List<Car> getCheapestCars();

    /** Active listings ordered by listing creation time descending; row count capped by {@code app.listing.car-catalog-limit}. */
    List<Car> getMostRecentCars();

    /** All cars for the owner (LEFT JOIN to non-finished listings), with the same filters as the owner listing grid. */
    Page<CarCard> getOwnerCarCards(OwnerListingSearchCriteria criteria);

    /** Updates a car's lifecycle status via dirty-checking (load + mutate). */
    void setCarStatus(long carId, Car.Status newStatus);

    /**
     * Returns at most {@code limit} active cars with the same type/powertrain/transmission as the reference car,
     * excluding the reference car and optionally the owner.
     */
    List<CarCard> findSimilarCarCards(long carId, int limit, LocalDate browseWallDate, Long excludeOwnerUserId);

    /** All cars for the owner whose status is one of {@code statuses}. */
    List<Car> findCarsByOwnerAndStatuses(long ownerId, java.util.Collection<Car.Status> statuses);

    /** All cars whose status equals {@code status}. */
    List<Car> findCarsByStatus(Car.Status status);

    /**
     * Atomically transitions a car from {@code expected} to {@code newStatus} only when its current
     * status matches {@code expected}. Returns {@code true} when the row was updated.
     */
    boolean updateCarStatusIfCurrent(long carId, Car.Status newStatus, Car.Status expected);
}
