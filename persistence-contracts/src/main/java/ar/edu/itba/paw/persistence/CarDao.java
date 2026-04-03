package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Car;

import java.util.List;
import java.util.Optional;

public interface CarDao {
    Car createCar(long ownerId, String plate, String brand, String model, Car.Type type, Car.Powertrain powertrain, Car.Transmission transmission);

    Optional<Car> getCarById(final long id);

    List<Car> getCheapestCars();

    List<Car> getMostRecentCars();
}
