package ar.edu.itba.paw.webapp.support;

import java.util.List;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.dto.GalleryMediaUpload;
import ar.edu.itba.paw.models.domain.car.CarPicture;
import ar.edu.itba.paw.services.car.CarGalleryMediaService;

/**
 * Thin HTTP-layer facade over {@link CarGalleryMediaService} for REST car publish / picture POST.
 */
@Component
public class CarGalleryUploadSupport {

    private final CarGalleryMediaService carGalleryMediaService;

    public CarGalleryUploadSupport(final CarGalleryMediaService carGalleryMediaService) {
        this.carGalleryMediaService = carGalleryMediaService;
    }

    public CarPicture attachSingleGalleryMedia(
            final long ownerId,
            final long carId,
            final GalleryMediaUpload media,
            final int displayOrder) {
        return carGalleryMediaService.attachSingleGalleryMedia(ownerId, carId, media, displayOrder);
    }

    public void attachGalleryMedia(
            final long ownerId,
            final long carId,
            final List<GalleryMediaUpload> galleryMedia,
            final int startingDisplayOrder) {
        carGalleryMediaService.attachGalleryMedia(ownerId, carId, galleryMedia, startingDisplayOrder);
    }

    public int nextDisplayOrder(final long carId) {
        return carGalleryMediaService.nextDisplayOrder(carId);
    }
}
