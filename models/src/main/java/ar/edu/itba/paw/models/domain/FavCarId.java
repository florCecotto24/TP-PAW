package ar.edu.itba.paw.models.domain;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/** Composite primary key for {@link FavCar}: a (car, user) pair is unique. */
@Embeddable
public class FavCarId implements Serializable {

    @Column(name = "car_id")
    private long carId;

    @Column(name = "user_id")
    private long userId;

    /* package */ FavCarId() {
        // For Hibernate
    }

    public FavCarId(final long carId, final long userId) {
        this.carId = carId;
        this.userId = userId;
    }

    public long getCarId() {
        return carId;
    }

    public long getUserId() {
        return userId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FavCarId)) {
            return false;
        }
        final FavCarId other = (FavCarId) o;
        return carId == other.carId && userId == other.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(carId, userId);
    }
}
