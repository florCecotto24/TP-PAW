package ar.edu.itba.paw.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.image.ImageValidationException;
import ar.edu.itba.paw.models.domain.CarPicture;
import ar.edu.itba.paw.persistence.CarPictureDao;

/** Gallery rows via {@link CarPictureDao}; {@link ImageService} guards {@code imageId} on create. */
@Service
public final class CarPictureServiceImpl implements CarPictureService {

    private final CarPictureDao carPictureDao;
    private final ImageService imageService;

    @Autowired
    public CarPictureServiceImpl(final CarPictureDao carPictureDao, final ImageService imageService) {
        this.carPictureDao = carPictureDao;
        this.imageService = imageService;
    }

    @Override
    @Transactional
    public CarPicture createCarPicture(final long carId, final long imageId, final int displayOrder) {
        if (imageId <= 0 || imageService.getImageById(imageId).isEmpty()) {
            throw new ImageValidationException(MessageKeys.IMAGE_INVALID_ID);
        }
        return carPictureDao.createCarPicture(carId, imageId, displayOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CarPicture> getCarPictureById(final long id) {
        return carPictureDao.getCarPictureById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarPicture> getCarPicturesByCarId(final long carId) {
        return carPictureDao.getCarPicturesByCarId(carId);
    }
}
