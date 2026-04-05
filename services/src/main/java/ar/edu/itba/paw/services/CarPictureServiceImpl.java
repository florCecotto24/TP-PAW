package ar.edu.itba.paw.services;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.image.ImageValidationException;
import ar.edu.itba.paw.models.CarPicture;
import ar.edu.itba.paw.persistence.CarPictureDao;
import org.springframework.stereotype.Service;

import java.util.List;
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
    public CarPicture createCarPicture(final long carId, final long imageId, final int displayOrder) {
        if (imageId <= 0 || imageService.getImageById(imageId).isEmpty()) {
            throw new ImageValidationException(MessageKeys.IMAGE_INVALID_ID);
        }
        return carPictureDao.createCarPicture(carId, imageId, displayOrder);
    }

    @Override
    public Optional<CarPicture> getCarPictureById(final long id) {
        return carPictureDao.getCarPictureById(id);
    }

    @Override
    public List<CarPicture> getCarPicturesByCarId(final long carId) {
        return carPictureDao.getCarPicturesByCarId(carId);
    }
}
