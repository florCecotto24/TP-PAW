package ar.edu.itba.paw.models.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

/**
 * Bridge between a {@link Reservation} and the {@link ListingAvailability} rows that priced each
 * wall-calendar chunk ({@code reservations_availabilities}).
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
    private ListingAvailability availability;

    @Column(name = "covered_end_date", nullable = false)
    private LocalDate coveredEndDate;

    /* package */ ReservationAvailabilityCoverage() {
        // For Hibernate
    }

    public ReservationAvailabilityCoverage(
            final Reservation reservation,
            final ListingAvailability availability,
            final LocalDate coveredStartDate,
            final LocalDate coveredEndDate) {
        this.reservation = Objects.requireNonNull(reservation);
        this.availability = Objects.requireNonNull(availability);
        this.id = new ReservationAvailabilityCoverageId(
                reservation.getId(), availability.getId(), coveredStartDate);
        this.coveredEndDate = Objects.requireNonNull(coveredEndDate);
        if (coveredEndDate.isBefore(coveredStartDate)) {
            throw new IllegalArgumentException("coveredEndDate must be on or after coveredStartDate");
        }
    }

    public ReservationAvailabilityCoverageId getId() {
        return id;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public ListingAvailability getAvailability() {
        return availability;
    }

    public LocalDate getCoveredStartDate() {
        return id.getCoveredStartDate();
    }

    public LocalDate getCoveredEndDate() {
        return coveredEndDate;
    }

    /** Inclusive wall-calendar days covered by this row. */
    public long coveredDaysInclusive() {
        return ChronoUnit.DAYS.between(getCoveredStartDate(), coveredEndDate.plusDays(1));
    }
}
