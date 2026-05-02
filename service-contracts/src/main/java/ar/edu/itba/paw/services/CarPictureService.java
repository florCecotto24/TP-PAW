package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.domain.CarPicture;

import java.util.List;
import java.util.Optional;

/**
 * Links {@link ar.edu.itba.paw.models.domain.Image} rows to a car with gallery display order.
 * Implementations use {@code CarPictureDao} only for gallery rows; {@code ImageService} validates that {@code imageId} exists before insert.
 */
public interface CarPictureService {
    /** Persists a car picture row referencing an existing image id. */
    CarPicture createCarPicture(final long carId, final long imageId, final int displayOrder);

    /** Loads one gallery row by id when present. */
    Optional<CarPicture> getCarPictureById(final long id);

    /** All pictures for a car ordered for display (typically ascending {@code display_order}). */
    List<CarPicture> getCarPicturesByCarId(final long carId);
}
