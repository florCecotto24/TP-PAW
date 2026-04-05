package ar.edu.itba.paw.services;

import ar.edu.itba.paw.dto.CarPublicationResult;
import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Car;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CarService {

    Car createCar(
            final long ownerId,
            final String plate,
            final String brand,
            final String model,
            final Car.Type type,
            final Car.Powertrain powertrain,
            final Car.Transmission transmission);

    Optional<Car> getCarById(final long id);

    List<Car> getCheapestCars();

    List<Car> getMostRecentCars();

    CarPublicationResult publish(
            final String ownerEmail,
            final String ownerName,
            final String ownerSurname,
            final String plate,
            final String brand,
            final String model,
            final Car.Type type,
            final Car.Powertrain powertrain,
            final Car.Transmission transmission,
            final BigDecimal pricePerDay,
            final String startPoint,
            final String description,
            final List<AvailabilityPeriod> periods,
            final List<ImageUpload> images);
}
