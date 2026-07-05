package ar.edu.itba.paw.services.car;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.image.ImageValidationException;
import ar.edu.itba.paw.models.domain.car.CarPicture;
import ar.edu.itba.paw.persistence.car.CarPictureDao;

import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.file.StoredFileService;
/** Gallery rows via {@link CarPictureDao}; validates referenced media ids on create. */
@Service
public class CarPictureServiceImpl implements CarPictureService {

    private final CarPictureDao carPictureDao;
    private final ImageService imageService;
    private final StoredFileService storedFileService;

    @Autowired
    public CarPictureServiceImpl(
            final CarPictureDao carPictureDao,
            final ImageService imageService,
            final StoredFileService storedFileService) {
        this.carPictureDao = carPictureDao;
        this.imageService = imageService;
        this.storedFileService = storedFileService;
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
    @Transactional
    public CarPicture createCarPictureFromVideo(
            final long carId, final long storedFileId, final int displayOrder) {
        if (storedFileId <= 0 || storedFileService.findById(storedFileId).isEmpty()) {
            throw new ImageValidationException(MessageKeys.IMAGE_INVALID_ID);
        }
        return carPictureDao.createCarPictureFromVideo(carId, storedFileId, displayOrder);
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

    @Override
    @Transactional(readOnly = true)
    public boolean isStoredFileInCarGallery(final long storedFileId) {
        return carPictureDao.isStoredFileInCarGallery(storedFileId);
    }

    @Override
    @Transactional
    public void deleteCarPictureForCar(final long carId, final long pictureId) {
        final Optional<CarPicture> pictureOpt = carPictureDao.getCarPictureById(pictureId);
        if (pictureOpt.isEmpty() || pictureOpt.get().getCarId() != carId) {
            throw new ImageValidationException(MessageKeys.IMAGE_INVALID_ID);
        }
        carPictureDao.deleteCarPicture(pictureId);
    }
}
