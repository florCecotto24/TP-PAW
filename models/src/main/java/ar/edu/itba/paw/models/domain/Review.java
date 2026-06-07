package ar.edu.itba.paw.models.domain;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import ar.edu.itba.paw.models.domain.internal.EntityEquality;

/** Review left by a rider or owner after a reservation. Composite PK: (reservation_id, made_by_rider). */
@Entity
@Table(name = "reviews")
public class Review {

    @EmbeddedId
    private ReviewId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("reservationId")
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column
    private Integer rating;

    @Column
    private String comment;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, optional = true)
    @JoinColumn(name = "image_id", unique = true)
    private Image image;

    /* package */ Review() {
        // For Hibernate
    }

    private Review(final Builder b) {
        this.id = new ReviewId(b.reservation.getId(), b.madeByRider);
        this.reservation = b.reservation;
        this.car = b.reservation.getCar();
        this.createdAt = b.createdAt;
        this.rating = b.rating;
        this.comment = b.comment;
        this.image = b.image;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Review} (Effective Java, Item 2: avoids the telescoping-constructor smell
     * for the optional {@code rating}, {@code comment} and {@code image} fields). The {@code car}
     * is intentionally not configurable: it is derived from the reservation so the (car_id, review)
     * invariant cannot be violated by callers.
     */
    public static final class Builder {
        private Reservation reservation;
        private boolean madeByRider;
        private OffsetDateTime createdAt;
        private Integer rating;
        private String comment;
        private Image image;

        public Builder reservation(final Reservation reservation) {
            this.reservation = reservation;
            return this;
        }

        public Builder madeByRider(final boolean madeByRider) {
            this.madeByRider = madeByRider;
            return this;
        }

        public Builder createdAt(final OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder rating(final Integer rating) {
            this.rating = rating;
            return this;
        }

        public Builder comment(final String comment) {
            this.comment = comment;
            return this;
        }

        public Builder image(final Image image) {
            this.image = image;
            return this;
        }

        public Review build() {
            Objects.requireNonNull(reservation, "reservation");
            Objects.requireNonNull(createdAt, "createdAt");
            return new Review(this);
        }
    }

    public ReviewId getId() {
        return id;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public Car getCar() {
        return car;
    }

    /** Convenience accessor for the car id of the underlying reservation. */
    public long getCarId() {
        return (car != null ? car : reservation.getCar()).getId();
    }

    public boolean isMadeByRider() {
        return id.isMadeByRider();
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public Optional<Integer> getRating() {
        return Optional.ofNullable(rating);
    }

    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    public Optional<Image> getImage() {
        return Optional.ofNullable(image);
    }

    public void setImage(final Image image) {
        this.image = image;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Review)) {
            return false;
        }
        return EntityEquality.equalsByEmbeddedId(this.id, ((Review) o).id);
    }

    @Override
    public int hashCode() {
        return EntityEquality.hashByEmbeddedId(this, id);
    }
}
