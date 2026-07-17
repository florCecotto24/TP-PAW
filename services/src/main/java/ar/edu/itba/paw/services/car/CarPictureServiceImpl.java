package ar.edu.itba.paw.services.car;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.image.ImageValidationException;
import ar.edu.itba.paw.models.domain.car.CarPicture;
import ar.edu.itba.paw.models.dto.car.CarPictureSummary;
import ar.edu.itba.paw.models.dto.Page;
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
    public Page<CarPicture> findByCarPaginated(
            final long carId, final int zeroBasedPage, final int pageSize) {
        final int safePage = Math.max(0, zeroBasedPage);
        final int safePageSize = Math.max(1, pageSize);
        final long total = carPictureDao.countByCarId(carId);
        if (total == 0L) {
            return new Page<>(List.of(), safePage, safePageSize, 0L);
        }
        final List<CarPicture> content = carPictureDao.findByCarIdOrderByDisplayOrderAsc(
                carId, safePage * safePageSize, safePageSize);
        return new Page<>(content, safePage, safePageSize, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarPictureSummary> findSummariesByCarPaginated(
            final long carId, final int zeroBasedPage, final int pageSize) {
        final int safePage = Math.max(0, zeroBasedPage);
        final int safePageSize = Math.max(1, pageSize);
        final long total = carPictureDao.countByCarId(carId);
        if (total == 0L) {
            return new Page<>(List.of(), safePage, safePageSize, 0L);
        }
        final List<CarPictureSummary> content = carPictureDao.findSummariesByCarIdOrderByDisplayOrderAsc(
                carId, safePage * safePageSize, safePageSize);
        return new Page<>(content, safePage, safePageSize, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CarPicture> findPrimaryPictureByCarId(final long carId) {
        return carPictureDao.findFirstByCarIdOrderByDisplayOrderAsc(carId);
    }

    @Override
    @Transactional(readOnly = true)
    public int findMaxDisplayOrderByCarId(final long carId) {
        return carPictureDao.findMaxDisplayOrderByCarId(carId).orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStoredFileInCarGallery(final long storedFileId) {
        return carPictureDao.isStoredFileInCarGallery(storedFileId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findCarIdsByImageId(final long imageId) {
        return carPictureDao.findCarIdsByImageId(imageId);
    }

    @Override
    @Transactional
    public void deleteCarPictureForCar(final long carId, final long pictureId) {
        final Optional<CarPicture> pictureOpt = carPictureDao.getCarPictureById(pictureId);
        if (pictureOpt.isEmpty() || pictureOpt.get().getCarId() != carId) {
            throw new ImageValidationException(MessageKeys.IMAGE_INVALID_ID);
        }
        final CarPicture picture = pictureOpt.get();
        final Long imageId = picture.getImageId();
        final Long storedFileId = picture.getStoredFileId();
        carPictureDao.deleteCarPicture(pictureId);
        if (imageId != null && imageId > 0) {
            imageService.deleteImage(imageId);
        }
        if (storedFileId != null && storedFileId > 0) {
            storedFileService.deleteById(storedFileId);
        }
    }
}
