package ar.edu.itba.paw.services;

import ar.edu.itba.paw.dto.CarPublicationResult;
import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Car;

import java.math.BigDecimal;
import java.util.List;

public interface CarPublicationService {

    CarPublicationResult publish(
            String ownerEmail,
            String ownerName,
            String ownerSurname,
            String plate,
            String brand,
            String model,
            Car.Type type,
            Car.Powertrain powertrain,
            Car.Transmission transmission,
            BigDecimal pricePerDay,
            String startPoint,
            String description,
            List<AvailabilityPeriod> periods,
            List<ImageUpload> images);
}
