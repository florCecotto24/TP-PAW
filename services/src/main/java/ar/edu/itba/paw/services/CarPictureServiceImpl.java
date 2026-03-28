package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.CarPicture;
import ar.edu.itba.paw.persistence.CarPictureDao;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CarPictureServiceImpl implements CarPictureService{

    private final CarPictureDao carPictureDao;
    private final ImageService imageService;

    public CarPictureServiceImpl(final CarPictureDao carPictureDao, final ImageService imageService) {
        this.carPictureDao = carPictureDao;
        this.imageService = imageService;
    }

    @Override
    public CarPicture createCarPicture(final long carId, final long imageId, final int order) {
        if (imageId <= 0 || imageService.getImageById(imageId).isEmpty()) {
            throw new IllegalArgumentException("Invalid image ID");
        }
        return carPictureDao.createCarPicture(carId, imageId, order);
    }

    @Override
    public Optional<CarPicture> getCarPictureById(final long id) {
        return carPictureDao.getCarPictureById(id);
    }
}
