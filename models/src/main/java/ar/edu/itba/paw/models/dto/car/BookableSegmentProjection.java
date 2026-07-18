package ar.edu.itba.paw.models.dto.car;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Effective per-day projection for the rider date-picker, grouped into contiguous wall-day segments where
 * every day shares the same effective attributes (price, check-in/out times, public location,
 * neighbourhood, and source availability).
 *
 * Because a car may have multiple overlapping {@code CarAvailability} rows, the day-effective
 * values come from the most recently created OFFERED availability that covers each day. Adjacent days
 * are merged into the same segment iff their full projection (including {@code availabilityId}) is
 * identical.
 *
 * All times are wall-clock in the car's wall zone. The {@code publicLocation} is a pre-formatted
 * single-line "street, neighborhood" string and does not include the door number.
 */
public final class BookableSegmentProjection {

    private final LocalDate from;
    private final LocalDate to;
    private final BigDecimal dayPrice;
    private final LocalTime checkInTime;
    private final LocalTime checkOutTime;
    private final String publicLocation;
    private final Long neighborhoodId;
    private final Long availabilityId;
    private final Long carId;

    public BookableSegmentProjection(
            final LocalDate from,
            final LocalDate to,
            final BigDecimal dayPrice,
            final LocalTime checkInTime,
            final LocalTime checkOutTime,
            final String publicLocation,
            final Long neighborhoodId,
            final Long availabilityId,
            final Long carId) {
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.dayPrice = dayPrice;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.publicLocation = publicLocation == null ? "" : publicLocation;
        this.neighborhoodId = neighborhoodId;
        this.availabilityId = availabilityId;
        this.carId = carId;
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be on or before to");
        }
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }

    public BigDecimal getDayPrice() {
        return dayPrice;
    }

    public LocalTime getCheckInTime() {
        return checkInTime;
    }

    public LocalTime getCheckOutTime() {
        return checkOutTime;
    }

    public String getPublicLocation() {
        return publicLocation;
    }

    public Long getNeighborhoodId() {
        return neighborhoodId;
    }

    public Long getAvailabilityId() {
        return availabilityId;
    }

    public Long getCarId() {
        return carId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BookableSegmentProjection)) {
            return false;
        }
        final BookableSegmentProjection that = (BookableSegmentProjection) o;
        return from.equals(that.from)
                && to.equals(that.to)
                && Objects.equals(dayPrice, that.dayPrice)
                && Objects.equals(checkInTime, that.checkInTime)
                && Objects.equals(checkOutTime, that.checkOutTime)
                && publicLocation.equals(that.publicLocation)
                && Objects.equals(neighborhoodId, that.neighborhoodId)
                && Objects.equals(availabilityId, that.availabilityId)
                && Objects.equals(carId, that.carId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                from, to, dayPrice, checkInTime, checkOutTime, publicLocation, neighborhoodId,
                availabilityId, carId);
    }

    @Override
    public String toString() {
        return "BookableSegmentProjection{"
                + "from=" + from
                + ", to=" + to
                + ", dayPrice=" + dayPrice
                + ", checkInTime=" + checkInTime
                + ", checkOutTime=" + checkOutTime
                + ", publicLocation='" + publicLocation + '\''
                + ", neighborhoodId=" + neighborhoodId
                + ", availabilityId=" + availabilityId
                + ", carId=" + carId
                + '}';
    }
}
