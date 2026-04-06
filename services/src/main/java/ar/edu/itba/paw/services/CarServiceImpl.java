package ar.edu.itba.paw.services;

import ar.edu.itba.paw.dto.CarPublicationResult;
import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.Image;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.CarDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class CarServiceImpl implements CarService {

    private final CarDao carDao;
    private final UserService userService;
    private final ListingService listingService;
    private final ImageService imageService;
    private final CarPictureService carPictureService;

    @Autowired
    public CarServiceImpl(
            final CarDao carDao,
            final UserService userService,
            final ListingService listingService,
            final ImageService imageService,
            final CarPictureService carPictureService) {
        this.carDao = carDao;
        this.userService = userService;
        this.listingService = listingService;
        this.imageService = imageService;
        this.carPictureService = carPictureService;
    }

    @Override
    public Car createCar(
            final long ownerId,
            final String plate,
            final String brand,
            final String model,
            final Car.Type type,
            final Car.Powertrain powertrain,
            final Car.Transmission transmission) {
        return carDao.createCar(ownerId, plate, brand, model, type, powertrain, transmission);
    }

    @Override
    public Optional<Car> getCarById(final long id) {
        return carDao.getCarById(id);
    }

    @Override
    public List<Car> getCheapestCars() {
        return carDao.getCheapestCars();
    }

    @Override
    public List<Car> getMostRecentCars() {
        return carDao.getMostRecentCars();
    }

    @Override
    @Transactional
    public CarPublicationResult publish(
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
            final LocalTime checkInTime,
            final LocalTime checkOutTime,
            final List<AvailabilityPeriod> periods,
            final List<ImageUpload> images) {
        final User publisher = userService.findOrCreatePublisher(ownerEmail, ownerName, ownerSurname);
        final Car car = createCar(
                publisher.getId(),
                plate,
                brand,
                model,
                type,
                powertrain,
                transmission);
        final Listing listing = listingService.createListing(
                car.getId(),
                Listing.Status.ACTIVE,
                pricePerDay,
                startPoint,
                description,
                checkInTime,
                checkOutTime,
                periods);

        int displayOrder = 1;
        if (images != null) {
            for (final ImageUpload picture : images) {
                if (picture.getData() == null || picture.getData().length == 0) {
                    continue;
                }
                final Image image = imageService.createImage(
                        picture.getFilename(),
                        picture.getContentType(),
                        picture.getData());
                carPictureService.createCarPicture(car.getId(), image.getId(), displayOrder);
                displayOrder++;
            }
        }

        return new CarPublicationResult(publisher, car, listing);
    }
}
