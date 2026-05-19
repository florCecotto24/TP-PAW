package ar.edu.itba.paw.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.exception.listing.DuplicatePlateException;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.persistence.CarDao;

@Service
public final class CarServiceImpl implements CarService {

    private final CarDao carDao;
    private final ImageService imageService;
    private final CarPictureService carPictureService;

    @Autowired
    public CarServiceImpl(
            final CarDao carDao,
            final ImageService imageService,
            final CarPictureService carPictureService) {
        this.carDao = carDao;
        this.imageService = imageService;
        this.carPictureService = carPictureService;
    }

    @Override
    @Transactional
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
    @Transactional
    public Car publishCar(
            final long ownerId,
            final String plate,
            final String brand,
            final String model,
            final Car.Type type,
            final Car.Powertrain powertrain,
            final Car.Transmission transmission,
            final List<ImageUpload> images) {
        if (carDao.existsByOwnerAndPlate(ownerId, plate)) {
            throw new DuplicatePlateException(plate);
        }
        final Car car = carDao.createCar(ownerId, plate, brand, model, type, powertrain, transmission);
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
        return car;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByOwnerAndPlate(final long ownerId, final String plate) {
        return carDao.existsByOwnerAndPlate(ownerId, plate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Car> getCarById(final long id) {
        return carDao.getCarById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Car> getCheapestCars() {
        return carDao.getCheapestCars();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Car> getMostRecentCars() {
        return carDao.getMostRecentCars();
    }
}
