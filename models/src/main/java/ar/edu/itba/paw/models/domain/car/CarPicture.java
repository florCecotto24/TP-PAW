package ar.edu.itba.paw.models.domain.car;

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

import ar.edu.itba.paw.models.domain.file.Image;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.internal.EntityEquality;

/** Ordered gallery link between a {@link Car} and a photo ({@link Image}) or video ({@link StoredFile}). */
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
    @JoinColumn(name = "image_id")
    private Image image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stored_file_id")
    private StoredFile storedFile;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /* package */ CarPicture() {
        // For Hibernate
    }

    /* package */ CarPicture(
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

    public static CarPicture forImage(
            final Car car,
            final Image image,
            final int displayOrder,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt) {
        final CarPicture picture = new CarPicture();
        picture.car = car;
        picture.image = image;
        picture.displayOrder = displayOrder;
        picture.createdAt = createdAt;
        picture.updatedAt = updatedAt;
        return picture;
    }

    /** Rehydrates a persisted photo row (tests and in-memory fixtures). */
    public static CarPicture identifiedForImage(
            final long id,
            final Car car,
            final Image image,
            final int displayOrder,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt) {
        return new CarPicture(id, car, image, displayOrder, createdAt, updatedAt);
    }

    public static CarPicture forVideo(
            final Car car,
            final StoredFile storedFile,
            final int displayOrder,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt) {
        final CarPicture picture = new CarPicture();
        picture.car = car;
        picture.storedFile = storedFile;
        picture.displayOrder = displayOrder;
        picture.createdAt = createdAt;
        picture.updatedAt = updatedAt;
        return picture;
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

    public StoredFile getStoredFile() {
        return storedFile;
    }

    public boolean isVideo() {
        return storedFile != null;
    }

    /** Convenience accessor — {@code null} when this row is a video. */
    public Long getImageId() {
        return image == null ? null : image.getId();
    }

    /** Convenience accessor — {@code null} when this row is a photo. */
    public Long getStoredFileId() {
        return storedFile == null ? null : storedFile.getId();
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
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CarPicture)) {
            return false;
        }
        return EntityEquality.equalsByLongId(this, this.id, ((CarPicture) o).id);
    }

    @Override
    public int hashCode() {
        return EntityEquality.hashByLongId(this, id);
    }

    @Override
    public String toString() {
        return "CarPicture{" +
                "id=" + id +
                ", carId=" + (car == null ? null : car.getId()) +
                ", imageId=" + getImageId() +
                ", storedFileId=" + getStoredFileId() +
                ", displayOrder=" + displayOrder +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
