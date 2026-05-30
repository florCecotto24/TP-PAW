package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.domain.CarPicture;
import java.util.List;
import java.util.Optional;

/** Join rows between cars and gallery media (photos or videos) with display order. */
public interface CarPictureDao {

    CarPicture createCarPicture(final long carId, final long imageId, final int displayOrder);

    CarPicture createCarPictureFromVideo(final long carId, final long storedFileId, final int displayOrder);

    Optional<CarPicture> getCarPictureById(final long id);

    List<CarPicture> getCarPicturesByCarId(final long carId);

    boolean isStoredFileInCarGallery(final long storedFileId);
}
