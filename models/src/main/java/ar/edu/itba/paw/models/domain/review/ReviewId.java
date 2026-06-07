package ar.edu.itba.paw.models.domain.review;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/** Composite primary key for {@link Review}: (reservation_id, made_by_rider). */
@Embeddable
public class ReviewId implements Serializable {

    @Column(name = "reservation_id")
    private long reservationId;

    @Column(name = "made_by_rider")
    private boolean madeByRider;

    /* package */ ReviewId() {
        // For Hibernate
    }

    public ReviewId(final long reservationId, final boolean madeByRider) {
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
        if (this == o) return true;
        if (!(o instanceof ReviewId)) return false;
        final ReviewId other = (ReviewId) o;
        return reservationId == other.reservationId && madeByRider == other.madeByRider;
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationId, madeByRider);
    }
}
