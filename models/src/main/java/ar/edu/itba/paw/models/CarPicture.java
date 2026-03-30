package ar.edu.itba.paw.models;

import java.time.OffsetDateTime;

public class CarPicture {
    private final long id;
    private final long carId;
    private final long imageId;
    private final int displayOrder;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public CarPicture(
            final long id,
            final long carId,
            final long imageId,
            final int displayOrder,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt) {
        this.id = id;
        this.carId = carId;
        this.imageId = imageId;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public long getCarId() {
        return carId;
    }

    public long getImageId() {
        return imageId;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "CarPicture{" +
                "id=" + id +
                ", carId=" + carId +
                ", imageId=" + imageId +
                ", displayOrder=" + displayOrder +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
