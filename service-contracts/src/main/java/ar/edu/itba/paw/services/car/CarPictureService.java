package ar.edu.itba.paw.services.car;

import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.car.CarPicture;

import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.file.StoredFileService;
/**
 * Links gallery media rows to a car with display order.
 * Implementations use {@code CarPictureDao}; {@link ImageService} and {@link StoredFileService} guard ids on create.
 */
public interface CarPictureService {
    /** Persists a car picture row referencing an existing image id. */
    CarPicture createCarPicture(final long carId, final long imageId, final int displayOrder);

    /** Persists a car gallery video row referencing an existing stored file id. */
    CarPicture createCarPictureFromVideo(final long carId, final long storedFileId, final int displayOrder);

    /** Loads one gallery row by id when present. */
    Optional<CarPicture> getCarPictureById(final long id);

    /** All pictures for a car ordered for display (typically ascending {@code display_order}). */
    List<CarPicture> getCarPicturesByCarId(final long carId);

    /** Whether the stored file is linked to a public car gallery video. */
    boolean isStoredFileInCarGallery(final long storedFileId);

    /** Removes a gallery row when it belongs to {@code carId}. */
    void deleteCarPictureForCar(final long carId, final long pictureId);
}
