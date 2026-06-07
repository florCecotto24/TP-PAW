package ar.edu.itba.paw.models.domain;

import java.util.Objects;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

import ar.edu.itba.paw.models.domain.internal.EntityEquality;

/**
 * N:N bridge between a {@link Reservation} and the {@link CarAvailability} rows considered when
 * pricing the reservation ({@code reservations_availabilities}). The per-day winner is resolved at
 * read time by filtering by date range and picking the {@code MAX(created_at)}; no per-row date
 * segmentation is persisted.
 */
@Entity
@Table(name = "reservations_availabilities")
public class ReservationAvailabilityCoverage {

    @EmbeddedId
    private ReservationAvailabilityCoverageId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("reservationId")
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("availabilityId")
    @JoinColumn(name = "availability_id")
    private CarAvailability availability;

    /* package */ ReservationAvailabilityCoverage() {
        // For Hibernate
    }

    public ReservationAvailabilityCoverage(
            final Reservation reservation, final CarAvailability availability) {
        this.reservation = Objects.requireNonNull(reservation);
        this.availability = Objects.requireNonNull(availability);
        this.id = new ReservationAvailabilityCoverageId(reservation.getId(), availability.getId());
    }

    public ReservationAvailabilityCoverageId getId() {
        return id;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public CarAvailability getAvailability() {
        return availability;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReservationAvailabilityCoverage)) {
            return false;
        }
        return EntityEquality.equalsByEmbeddedId(this.id, ((ReservationAvailabilityCoverage) o).id);
    }

    @Override
    public int hashCode() {
        return EntityEquality.hashByEmbeddedId(this, id);
    }
}
