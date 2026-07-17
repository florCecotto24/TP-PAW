package ar.edu.itba.paw.services.car;

import java.util.List;

import ar.edu.itba.paw.dto.GalleryMediaUpload;
import ar.edu.itba.paw.models.domain.car.CarPicture;

/**
 * Gallery media attach for publish / picture flows.
 * Uses {@link ar.edu.itba.paw.services.file.ImageService}, {@link ar.edu.itba.paw.services.file.StoredFileService},
 * and {@link CarPictureService} only — no {@code CarDao}.
 */
public interface CarGalleryMediaService {

    int countNonEmptyGalleryUploads(List<GalleryMediaUpload> galleryMedia);

    CarPicture attachSingleGalleryMedia(
            long ownerId, long carId, GalleryMediaUpload media, int displayOrder);

    void attachGalleryMedia(
            long ownerId, long carId, List<GalleryMediaUpload> galleryMedia, int startingDisplayOrder);

    int nextDisplayOrder(long carId);
}
