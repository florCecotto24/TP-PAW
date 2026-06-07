package ar.edu.itba.paw.persistence.car;

import ar.edu.itba.paw.models.domain.car.CarPicture;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Join rows between cars and gallery media (photos or videos) with display order. */
public interface CarPictureDao {

    CarPicture createCarPicture(final long carId, final long imageId, final int displayOrder);

    CarPicture createCarPictureFromVideo(final long carId, final long storedFileId, final int displayOrder);

    Optional<CarPicture> getCarPictureById(final long id);

    List<CarPicture> getCarPicturesByCarId(final long carId);

    boolean isStoredFileInCarGallery(final long storedFileId);

    /**
     * Cover image id (lowest {@code displayOrder} with a non-null {@code imageId}) for each given
     * car. Cars without any picture are absent from the returned map.
     */
    Map<Long, Long> findCoverImageIdsByCarIds(Collection<Long> carIds);
}
