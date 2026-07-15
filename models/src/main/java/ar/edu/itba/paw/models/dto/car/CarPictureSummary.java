package ar.edu.itba.paw.models.dto.car;

/**
 * Lightweight projection of a car gallery item for the paginated list view. Carries only the metadata
 * the gallery representation needs (id, order, media kind + content type) and deliberately NOT the
 * binary {@code byte[]}: the list query builds this via {@code SELECT NEW} with a plain {@code LEFT JOIN}
 * (not {@code JOIN FETCH}), so the image/stored-file blobs are never materialized. The bytes are fetched
 * lazily, on demand, only by the dedicated per-picture binary endpoint.
 */
public final class CarPictureSummary {

    private final long id;
    private final long carId;
    private final int displayOrder;
    private final String imageContentType;
    private final String storedFileContentType;

    public CarPictureSummary(
            final long id,
            final long carId,
            final int displayOrder,
            final String imageContentType,
            final String storedFileContentType) {
        this.id = id;
        this.carId = carId;
        this.displayOrder = displayOrder;
        this.imageContentType = imageContentType;
        this.storedFileContentType = storedFileContentType;
    }

    public long getId() {
        return id;
    }

    public long getCarId() {
        return carId;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public String getStoredFileContentType() {
        return storedFileContentType;
    }

    /** A gallery row is a video iff it is backed by a stored file (mirrors {@code CarPicture#isVideo}). */
    public boolean isVideo() {
        return storedFileContentType != null;
    }
}
