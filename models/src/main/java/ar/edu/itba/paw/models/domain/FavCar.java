package ar.edu.itba.paw.models.domain;

import java.time.OffsetDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

import ar.edu.itba.paw.models.domain.internal.EntityEquality;

/**
 * Marks a {@link Car} as favorited by a {@link User}. Each (car, user) pair appears at most once
 * (enforced by the composite PK). The {@link #favoritedAt} timestamp drives the ordering of the
 * "Mis favoritos" listing (most-recently favorited first).
 */
@Entity
@Table(name = "fav_cars")
public class FavCar {

    @EmbeddedId
    private FavCarId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("carId")
    @JoinColumn(name = "car_id")
    private Car car;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "favorited_at", nullable = false)
    private OffsetDateTime favoritedAt;

    /* package */ FavCar() {
        // For Hibernate
    }

    public FavCar(final Car car, final User user, final OffsetDateTime favoritedAt) {
        this.car = Objects.requireNonNull(car);
        this.user = Objects.requireNonNull(user);
        this.favoritedAt = Objects.requireNonNull(favoritedAt);
        this.id = new FavCarId(car.getId(), user.getId());
    }

    public FavCarId getId() {
        return id;
    }

    public Car getCar() {
        return car;
    }

    public User getUser() {
        return user;
    }

    public OffsetDateTime getFavoritedAt() {
        return favoritedAt;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FavCar)) {
            return false;
        }
        return EntityEquality.equalsByEmbeddedId(this.id, ((FavCar) o).id);
    }

    @Override
    public int hashCode() {
        return EntityEquality.hashByEmbeddedId(this, id);
    }
}
