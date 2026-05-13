package ar.edu.itba.paw.models.domain;

import java.time.OffsetDateTime;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

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

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column
    private Integer rating;

    @Column
    private String comment;

    /* package */ Review() {
        // For Hibernate
    }

    public Review(
            final Reservation reservation,
            final boolean madeByRider,
            final OffsetDateTime createdAt,
            final Integer rating,
            final String comment) {
        this.id = new ReviewId(reservation.getId(), madeByRider);
        this.reservation = reservation;
        this.createdAt = createdAt;
        this.rating = rating;
        this.comment = comment;
    }

    public ReviewId getId() {
        return id;
    }

    public Reservation getReservation() {
        return reservation;
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
}
