package ar.edu.itba.paw.models.domain;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Persisted rider/owner review row for a reservation (maps {@code reviews}).
 * Used by JPA queries; business APIs may still expose DTOs.
 */
@Entity
@Table(name = "reviews")
public class ReservationReview {

    @EmbeddedId
    private ReservationReviewPk id;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "comment", length = 500)
    private String comment;

    /* package */ ReservationReview() {
    }

    public ReservationReviewPk getId() {
        return id;
    }

    public long getReservationId() {
        return id != null ? id.getReservationId() : 0L;
    }

    public boolean isMadeByRider() {
        return id != null && id.isMadeByRider();
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public Integer getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    /**
     * Builds a transient row for {@link javax.persistence.EntityManager#persist(Object)}.
     */
    public static ReservationReview newForInsert(
            final long reservationId,
            final boolean madeByRider,
            final OffsetDateTime createdAt,
            final Integer rating,
            final String comment) {
        final ReservationReview r = new ReservationReview();
        r.id = new ReservationReviewPk(reservationId, madeByRider);
        r.createdAt = createdAt;
        r.rating = rating;
        r.comment = comment;
        return r;
    }

    /** Composite primary key for {@link ReservationReview} (table {@code reviews}). */
    @Embeddable
    public static final class ReservationReviewPk implements Serializable {

        private static final long serialVersionUID = 1L;

        @Column(name = "reservation_id", nullable = false)
        private long reservationId;

        @Column(name = "made_by_rider", nullable = false)
        private boolean madeByRider;

        /* package */ ReservationReviewPk() {
        }

        public ReservationReviewPk(final long reservationId, final boolean madeByRider) {
            this.reservationId = reservationId;
            this.madeByRider = madeByRider;
        }

        public long getReservationId() {
            return reservationId;
        }

        public boolean isMadeByRider() {
            return madeByRider;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final ReservationReviewPk that = (ReservationReviewPk) o;
            return reservationId == that.reservationId && madeByRider == that.madeByRider;
        }

        @Override
        public int hashCode() {
            return Objects.hash(reservationId, madeByRider);
        }
    }
}
