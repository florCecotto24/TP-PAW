package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Car;

import java.util.Optional;

public interface CarService {

    Car createCar(final long ownerId, final String plate, final String brand, final String model, final Car.Type type, final Car.Powertrain powertrain, final Car.Trasnmission transmission);

    Optional<Car> getCarById(final long id);
}
