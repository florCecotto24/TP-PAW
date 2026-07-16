package ar.edu.itba.paw.webapp.support;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.dto.GalleryMediaUpload;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.CarValidationException;
import ar.edu.itba.paw.models.domain.car.CarPicture;
import ar.edu.itba.paw.models.domain.file.Image;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.util.media.CarGalleryMediaContentTypes;
import ar.edu.itba.paw.services.car.CarPictureService;
import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.file.StoredFileService;

/** Persists gallery uploads for REST car publish / picture POST flows. */
@Component
public class CarGalleryUploadSupport {

    private final ImageService imageService;
    private final StoredFileService storedFileService;
    private final CarPictureService carPictureService;

    public CarGalleryUploadSupport(
            final ImageService imageService,
            final StoredFileService storedFileService,
            final CarPictureService carPictureService) {
        this.imageService = imageService;
        this.storedFileService = storedFileService;
        this.carPictureService = carPictureService;
    }

    @Transactional
    public CarPicture attachSingleGalleryMedia(
            final long ownerId,
            final long carId,
            final GalleryMediaUpload media,
            final int displayOrder) {
        if (media.getData() == null || media.getData().length == 0) {
            throw new CarValidationException(MessageKeys.CAR_GALLERY_MEDIA_INVALID_TYPE);
        }
        if (CarGalleryMediaContentTypes.isImageContentType(media.getContentType())) {
            final Image image = imageService.createImage(
                    media.getFilename(), media.getContentType(), media.getData());
            return carPictureService.createCarPicture(carId, image.getId(), displayOrder);
        }
        if (CarGalleryMediaContentTypes.isVideoContentType(media.getContentType(), media.getFilename())) {
            final StoredFile video = storedFileService.create(
                    ownerId,
                    media.getFilename() != null ? media.getFilename() : "video",
                    media.getContentType() != null ? media.getContentType() : "video/mp4",
                    media.getData());
            return carPictureService.createCarPictureFromVideo(carId, video.getId(), displayOrder);
        }
        throw new CarValidationException(MessageKeys.CAR_GALLERY_MEDIA_INVALID_TYPE);
    }

    @Transactional
    public void attachGalleryMedia(
            final long ownerId,
            final long carId,
            final List<GalleryMediaUpload> galleryMedia,
            final int startingDisplayOrder) {
        if (galleryMedia == null || galleryMedia.isEmpty()) {
            return;
        }
        int displayOrder = startingDisplayOrder;
        for (final GalleryMediaUpload media : galleryMedia) {
            if (media.getData() == null || media.getData().length == 0) {
                continue;
            }
            attachSingleGalleryMedia(ownerId, carId, media, displayOrder++);
        }
    }

    public int nextDisplayOrder(final long carId) {
        return carPictureService.findMaxDisplayOrderByCarId(carId) + 1;
    }
}
