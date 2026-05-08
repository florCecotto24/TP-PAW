package ar.edu.itba.paw.models.domain;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/** Ordered gallery link between a {@link Car} and an {@link Image} id for listing photos. */
@Entity
@Table(name = "car_pictures")
public class CarPicture {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "car_pictures_id_seq")
    @SequenceGenerator(name = "car_pictures_id_seq", sequenceName = "car_pictures_id_seq", allocationSize = 1)
    private long id;

    @Column(name = "car_id", nullable = false)
    private long carId;

    @Column(name = "image_id", nullable = false)
    private long imageId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /* package */ CarPicture() {
        // For Hibernate
    }

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

    public CarPicture(
            final long carId,
            final long imageId,
            final int displayOrder,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt) {
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
