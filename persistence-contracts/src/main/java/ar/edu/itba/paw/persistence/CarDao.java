package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.util.OwnerListingSearchCriteria;

import java.util.List;
import java.util.Optional;

/** JPA-backed access to {@code cars} and catalogue queries joined to active listings. */
public interface CarDao {
    Car createCar(long ownerId, String plate, String brand, String model, Car.Type type, Car.Powertrain powertrain, Car.Transmission transmission);

    boolean existsByOwnerAndPlate(long ownerId, String plate);

    Optional<Car> getCarById(final long id);

    /** Active listings ordered by ascending day price; row count capped by {@code app.listing.car-catalog-limit}. */
    List<Car> getCheapestCars();

    /** Active listings ordered by listing creation time descending; row count capped by {@code app.listing.car-catalog-limit}. */
    List<Car> getMostRecentCars();

    /** All cars for the owner (LEFT JOIN to non-finished listings), with the same filters as the owner listing grid. */
    Page<CarCard> getOwnerCarCards(OwnerListingSearchCriteria criteria);
}
