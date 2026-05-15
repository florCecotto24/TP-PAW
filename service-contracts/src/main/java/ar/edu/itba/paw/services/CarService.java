package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.domain.Car;

import java.util.List;
import java.util.Optional;

/**
 * Car rows for owners; catalog slices use {@code app.listing.car-catalog-limit} (not UI pagination).
 * Implementations use {@code CarDao} only.
 */
public interface CarService {

    /** Persists a car for {@code ownerId} with normalized plate and validated enums. */
    Car createCar(
            final long ownerId,
            final String plate,
            final String brand,
            final String model,
            final Car.Type type,
            final Car.Powertrain powertrain,
            final Car.Transmission transmission);

    /** Returns true if the owner already has a car registered with the given plate. */
    boolean existsByOwnerAndPlate(long ownerId, String plate);

    /** Loads a car by primary key when present. */
    Optional<Car> getCarById(final long id);

    /** Cars with an {@code active} listing, ordered by ascending listing day price (row cap {@code app.listing.car-catalog-limit}). */
    List<Car> getCheapestCars();

    /** Cars with an {@code active} listing, ordered by listing creation time descending (row cap {@code app.listing.car-catalog-limit}). */
    List<Car> getMostRecentCars();
}
