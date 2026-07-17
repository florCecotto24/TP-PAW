package ar.edu.itba.paw.persistence.car;

import ar.edu.itba.paw.models.domain.car.CarPicture;
import ar.edu.itba.paw.models.dto.car.CarPictureSummary;
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

    long countByCarId(long carId);

    List<CarPicture> findByCarIdOrderByDisplayOrderAsc(long carId, int offset, int limit);

    /**
     * Metadata-only projection of a gallery page (no blobs materialized). Used by the paginated list
     * endpoint, which only needs id/order/content-type. The bytes are served separately per picture.
     */
    List<CarPictureSummary> findSummariesByCarIdOrderByDisplayOrderAsc(long carId, int offset, int limit);

    Optional<CarPicture> findFirstByCarIdOrderByDisplayOrderAsc(long carId);

    Optional<Integer> findMaxDisplayOrderByCarId(long carId);

    boolean isStoredFileInCarGallery(final long storedFileId);

    /**
     * Car ids whose gallery rows reference {@code imageId}.
     * Scalar id projection only — does not load {@code CarPicture} rows or image/video blobs.
     */
    List<Long> findCarIdsByImageId(final long imageId);

    /**
     * Cover image id (lowest {@code displayOrder} with a non-null {@code imageId}) for each given
     * car. Cars without any picture are absent from the returned map.
     */
    Map<Long, Long> findCoverImageIdsByCarIds(Collection<Long> carIds);

    /** Deletes a gallery row by primary key when present. */
    void deleteCarPicture(long id);
}
