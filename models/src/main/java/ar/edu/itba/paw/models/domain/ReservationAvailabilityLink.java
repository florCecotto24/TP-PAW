package ar.edu.itba.paw.models.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * One contiguous wall-calendar chunk of a reservation covered by a single effective
 * {@code listing_availability} row. Persisted in {@code reservations_availabilities}.
 */
public final class ReservationAvailabilityLink {

    private final long availabilityId;
    private final LocalDate coveredStartDate;
    private final LocalDate coveredEndDate;

    public ReservationAvailabilityLink(
            final long availabilityId,
            final LocalDate coveredStartDate,
            final LocalDate coveredEndDate) {
        if (coveredEndDate.isBefore(coveredStartDate)) {
            throw new IllegalArgumentException("coveredEndDate must be on or after coveredStartDate");
        }
        this.availabilityId = availabilityId;
        this.coveredStartDate = Objects.requireNonNull(coveredStartDate);
        this.coveredEndDate = Objects.requireNonNull(coveredEndDate);
    }

    public long getAvailabilityId() {
        return availabilityId;
    }

    public LocalDate getCoveredStartDate() {
        return coveredStartDate;
    }

    public LocalDate getCoveredEndDate() {
        return coveredEndDate;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReservationAvailabilityLink)) {
            return false;
        }
        final ReservationAvailabilityLink that = (ReservationAvailabilityLink) o;
        return availabilityId == that.availabilityId
                && coveredStartDate.equals(that.coveredStartDate)
                && coveredEndDate.equals(that.coveredEndDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(availabilityId, coveredStartDate, coveredEndDate);
    }

    @Override
    public String toString() {
        return "ReservationAvailabilityLink{availabilityId="
                + availabilityId
                + ", coveredStartDate="
                + coveredStartDate
                + ", coveredEndDate="
                + coveredEndDate
                + '}';
    }
}
