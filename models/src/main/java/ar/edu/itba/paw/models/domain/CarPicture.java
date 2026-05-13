package ar.edu.itba.paw.models.domain;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/** Ordered gallery link between a {@link Car} and an {@link Image} for listing photos. */
@Entity
@Table(name = "car_pictures")
public class CarPicture {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "car_pictures_id_seq")
    @SequenceGenerator(name = "car_pictures_id_seq", sequenceName = "car_pictures_id_seq", allocationSize = 1)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

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
            final Car car,
            final Image image,
            final int displayOrder,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt) {
        this.id = id;
        this.car = car;
        this.image = image;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public CarPicture(
            final Car car,
            final Image image,
            final int displayOrder,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt) {
        this.car = car;
        this.image = image;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public Car getCar() {
        return car;
    }

    /** Convenience accessor — returns {@code car.getId()}. */
    public long getCarId() {
        return car.getId();
    }

    public Image getImage() {
        return image;
    }

    /** Convenience accessor — returns {@code image.getId()}. */
    public long getImageId() {
        return image.getId();
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
                ", carId=" + car.getId() +
                ", imageId=" + image.getId() +
                ", displayOrder=" + displayOrder +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
