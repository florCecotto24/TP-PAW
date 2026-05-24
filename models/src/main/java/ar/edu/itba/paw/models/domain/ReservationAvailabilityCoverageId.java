package ar.edu.itba.paw.models.domain;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/** Composite primary key for {@link ReservationAvailabilityCoverage}. */
@Embeddable
public class ReservationAvailabilityCoverageId implements Serializable {

    @Column(name = "reservation_id")
    private long reservationId;

    @Column(name = "availability_id")
    private long availabilityId;

    /* package */ ReservationAvailabilityCoverageId() {
        // For Hibernate
    }

    public ReservationAvailabilityCoverageId(final long reservationId, final long availabilityId) {
        this.reservationId = reservationId;
        this.availabilityId = availabilityId;
    }

    public long getReservationId() {
        return reservationId;
    }

    public long getAvailabilityId() {
        return availabilityId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReservationAvailabilityCoverageId)) {
            return false;
        }
        final ReservationAvailabilityCoverageId other = (ReservationAvailabilityCoverageId) o;
        return reservationId == other.reservationId && availabilityId == other.availabilityId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationId, availabilityId);
    }
}
