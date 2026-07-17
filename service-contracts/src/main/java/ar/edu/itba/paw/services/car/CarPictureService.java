package ar.edu.itba.paw.services.car;

import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.car.CarPicture;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.CarPictureSummary;

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

    /** Gallery rows for one SQL page (ascending {@code display_order}). */
    Page<CarPicture> findByCarPaginated(long carId, int zeroBasedPage, int pageSize);

    /** Metadata-only paginated gallery (no blobs loaded); backs the list endpoint. */
    Page<CarPictureSummary> findSummariesByCarPaginated(long carId, int zeroBasedPage, int pageSize);

    /** First gallery item by display order (cover / primary thumbnail). */
    Optional<CarPicture> findPrimaryPictureByCarId(final long carId);

    /** Highest {@code displayOrder} in the gallery, or {@code 0} when empty. */
    int findMaxDisplayOrderByCarId(long carId);

    /** Whether the stored file is linked to a public car gallery video. */
    boolean isStoredFileInCarGallery(final long storedFileId);

    /**
     * Car ids whose galleries reference {@code imageId}; empty for non-gallery images.
     * Id-only; does not materialize gallery rows or image bytes.
     */
    List<Long> findCarIdsByImageId(final long imageId);

    /** Removes a gallery row when it belongs to {@code carId}. */
    void deleteCarPictureForCar(final long carId, final long pictureId);
}
