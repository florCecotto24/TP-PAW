package ar.edu.itba.paw.services;

import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.util.OwnerListingSearchCriteria;

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

    /**
     * Creates a car and saves its pictures in one transaction (step 1 of the two-step publish flow).
     * Duplicate plate check is performed here. CBU is NOT checked — that belongs to step 2.
     */
    Car publishCar(
            long ownerId,
            String plate,
            String brand,
            String model,
            Car.Type type,
            Car.Powertrain powertrain,
            Car.Transmission transmission,
            List<ImageUpload> images);

    /** Returns true if the owner already has a car registered with the given plate. */
    boolean existsByOwnerAndPlate(long ownerId, String plate);

    /** Loads a car by primary key when present. */
    Optional<Car> getCarById(final long id);

    /** Cars with an {@code active} listing, ordered by ascending listing day price (row cap {@code app.listing.car-catalog-limit}). */
    List<Car> getCheapestCars();

    /** Cars with an {@code active} listing, ordered by listing creation time descending (row cap {@code app.listing.car-catalog-limit}). */
    List<Car> getMostRecentCars();

    /** All cars for the owner (with or without an active/paused listing), paginated and filtered. */
    Page<CarCard> getOwnerCarCards(OwnerListingSearchCriteria criteria);
}
