package ar.edu.itba.paw.models.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import ar.edu.itba.paw.models.domain.internal.EntityEquality;

/**
 * Wall-calendar inclusive availability segment for a {@link Car}: carries its own pricing, pickup
 * location and check-in/out times. Rows are immutable from the owner's point of view; "edits" insert
 * new rows. When two rows overlap the same calendar day, the one with the most recent {@code createdAt}
 * wins (see {@link Kind} for the offered/withdrawn discriminator).
 */
@Entity
@Table(name = "car_availability")
public class CarAvailability {

    /**
     * Default wall-time pickup hour used as fallback when an availability row, publication form, or rider
     * pickup-lead calculation does not provide an explicit check-in time.
     */
    public static final LocalTime DEFAULT_CHECK_IN_TIME = LocalTime.of(10, 0);

    /**
     * Default wall-time return hour used as fallback when an availability row does not provide an
     * explicit check-out time (e.g. car detail UI for a car with no published availability yet).
     */
    public static final LocalTime DEFAULT_CHECK_OUT_TIME = LocalTime.of(20, 0);

    /**
     * Discriminator for layered availability rows:
     * <ul>
     *   <li>{@code OFFERED}: the days in {@code [startInclusive, endInclusive]} are bookable at {@code dayPrice}.</li>
     *   <li>{@code WITHDRAWN}: the days are explicitly removed from the calendar (used when an "edit"
     *       narrows a previous offered window).</li>
     * </ul>
     * The winner for any given day is the row with the most recent {@code createdAt} that contains it.
     */
    public enum Kind {
        OFFERED, WITHDRAWN
    }

    @Converter
    public static class KindConverter implements AttributeConverter<Kind, String> {
        @Override
        public String convertToDatabaseColumn(final Kind attribute) {
            return attribute == null ? null : attribute.name().toLowerCase();
        }
        @Override
        public Kind convertToEntityAttribute(final String dbData) {
            return dbData == null ? null : Kind.valueOf(dbData.toUpperCase());
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "car_availability_id_seq")
    @SequenceGenerator(name = "car_availability_id_seq", sequenceName = "car_availability_id_seq", allocationSize = 1)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @Column(name = "start_date", nullable = false)
    private LocalDate startInclusive;

    @Column(name = "end_date", nullable = false)
    private LocalDate endInclusive;

    @Column(name = "day_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal dayPrice;

    @Column(name = "start_point_street", nullable = false, length = 50)
    private String startPointStreet;

    @Column(name = "start_point_number", length = 10)
    private String startPointNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "neighborhood_id")
    private Neighborhood neighborhood;

    @Column(name = "check_in_time", nullable = false)
    private LocalTime checkInTime;

    @Column(name = "check_out_time", nullable = false)
    private LocalTime checkOutTime;

    @Convert(converter = KindConverter.class)
    @Column(nullable = false, length = 20)
    private Kind kind;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /* package */ CarAvailability() {
        // For Hibernate
    }

    private CarAvailability(final Builder b) {
        this.id = b.id;
        this.car = b.car;
        this.startInclusive = b.startInclusive;
        this.endInclusive = b.endInclusive;
        this.dayPrice = b.dayPrice;
        this.startPointStreet = b.startPointStreet;
        this.startPointNumber = b.startPointNumber;
        this.neighborhood = b.neighborhood;
        this.checkInTime = b.checkInTime;
        this.checkOutTime = b.checkOutTime;
        // Transitional defaults during Phase 2: legacy constructors and partial builders inherit
        // sensible values so existing tests keep working. Phase 5 will rewire all call sites and
        // these defaults can move to strict requireNonNull in build().
        this.kind = b.kind != null ? b.kind : Kind.OFFERED;
        final OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = b.createdAt != null ? b.createdAt : now;
        this.updatedAt = b.updatedAt != null ? b.updatedAt : this.createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long id;
        private Car car;
        private LocalDate startInclusive;
        private LocalDate endInclusive;
        private BigDecimal dayPrice;
        private String startPointStreet;
        private String startPointNumber;
        private Neighborhood neighborhood;
        private LocalTime checkInTime;
        private LocalTime checkOutTime;
        private Kind kind;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public Builder id(final long id) {
            this.id = id;
            return this;
        }

        public Builder car(final Car car) {
            this.car = car;
            return this;
        }

        public Builder startInclusive(final LocalDate startInclusive) {
            this.startInclusive = startInclusive;
            return this;
        }

        public Builder endInclusive(final LocalDate endInclusive) {
            this.endInclusive = endInclusive;
            return this;
        }

        public Builder dayPrice(final BigDecimal dayPrice) {
            this.dayPrice = dayPrice;
            return this;
        }

        public Builder startPointStreet(final String startPointStreet) {
            this.startPointStreet = startPointStreet;
            return this;
        }

        public Builder startPointNumber(final String startPointNumber) {
            this.startPointNumber = startPointNumber;
            return this;
        }

        public Builder neighborhood(final Neighborhood neighborhood) {
            this.neighborhood = neighborhood;
            return this;
        }

        public Builder checkInTime(final LocalTime checkInTime) {
            this.checkInTime = checkInTime;
            return this;
        }

        public Builder checkOutTime(final LocalTime checkOutTime) {
            this.checkOutTime = checkOutTime;
            return this;
        }

        public Builder kind(final Kind kind) {
            this.kind = kind;
            return this;
        }

        public Builder createdAt(final OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(final OffsetDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public CarAvailability build() {
            Objects.requireNonNull(car, "car");
            Objects.requireNonNull(startInclusive, "startInclusive");
            Objects.requireNonNull(endInclusive, "endInclusive");
            return new CarAvailability(this);
        }
    }

    public long getId() {
        return id;
    }

    public Car getCar() {
        return car;
    }

    public long getCarId() {
        return car.getId();
    }

    public LocalDate getStartInclusive() {
        return startInclusive;
    }

    public LocalDate getEndInclusive() {
        return endInclusive;
    }

    public Optional<BigDecimal> getDayPrice() {
        return Optional.ofNullable(dayPrice);
    }

    public BigDecimal getDayPriceValue() {
        return dayPrice;
    }

    public String getStartPointStreet() {
        return startPointStreet;
    }

    public Optional<String> getStartPointNumber() {
        return Optional.ofNullable(startPointNumber).filter(s -> !s.isBlank());
    }

    public Neighborhood getNeighborhood() {
        return neighborhood;
    }

    /** Convenience accessor — returns the neighborhood id, or empty if no neighborhood is set. */
    public Optional<Long> getNeighborhoodId() {
        return neighborhood == null ? Optional.empty() : Optional.of(neighborhood.getId());
    }

    public LocalTime getCheckInTime() {
        return checkInTime;
    }

    public LocalTime getCheckOutTime() {
        return checkOutTime;
    }

    public Kind getKind() {
        return kind;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CarAvailability)) {
            return false;
        }
        return EntityEquality.equalsByLongId(this, this.id, ((CarAvailability) o).id);
    }

    @Override
    public int hashCode() {
        return EntityEquality.hashByLongId(this, id);
    }

    @Override
    public String toString() {
        return "CarAvailability{"
                + "id=" + id
                + ", carId=" + (car != null ? car.getId() : null)
                + ", startInclusive=" + startInclusive
                + ", endInclusive=" + endInclusive
                + ", dayPrice=" + Objects.toString(dayPrice)
                + ", kind=" + kind
                + ", createdAt=" + createdAt
                + ", updatedAt=" + updatedAt
                + '}';
    }
}
