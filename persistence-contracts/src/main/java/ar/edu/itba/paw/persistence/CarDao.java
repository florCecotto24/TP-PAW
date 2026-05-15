package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.domain.Car;

import java.util.List;
import java.util.Optional;

/** JDBC-backed access to {@code cars} and catalogue queries joined to active listings. */
public interface CarDao {
    Car createCar(long ownerId, String plate, String brand, String model, Car.Type type, Car.Powertrain powertrain, Car.Transmission transmission);

    boolean existsByOwnerAndPlate(long ownerId, String plate);

    Optional<Car> getCarById(final long id);

    /** Active listings ordered by ascending day price; row count capped by {@code app.listing.car-catalog-limit}. */
    List<Car> getCheapestCars();

    /** Active listings ordered by listing creation time descending; row count capped by {@code app.listing.car-catalog-limit}. */
    List<Car> getMostRecentCars();
}
