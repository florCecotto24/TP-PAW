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

/**
 * Wall-calendar inclusive availability segment for a {@link Listing}: carries its own pricing, pickup
 * location and check-in/out times. Rows are immutable from the owner's point of view; "edits" insert
 * new rows. When two rows overlap the same calendar day, the one with the most recent {@code createdAt}
 * wins (see {@link Kind} for the offered/withdrawn discriminator).
 */
@Entity
@Table(name = "listing_availability")
public class ListingAvailability {

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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "listing_availability_id_seq")
    @SequenceGenerator(name = "listing_availability_id_seq", sequenceName = "listing_availability_id_seq", allocationSize = 1)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = true)
    private Listing listing;

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

    /* package */ ListingAvailability() {
        // For Hibernate
    }

    /**
     * Legacy 6-arg constructor kept for tests created before Phase 2. New code should use
     * {@link #builder()} so pricing, location and check-in/out times are explicit.
     *
     * @deprecated transitional; the builder will be the only public constructor after Phase 5.
     */
    @Deprecated
    public ListingAvailability(
            final long id,
            final Listing listing,
            final LocalDate startInclusive,
            final LocalDate endInclusive,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt) {
        this(builder()
                .id(id)
                .listing(listing)
                .startInclusive(startInclusive)
                .endInclusive(endInclusive)
                .createdAt(createdAt)
                .updatedAt(updatedAt));
    }

    /**
     * Legacy 6-arg constructor with explicit {@code dayPrice} kept for older tests.
     *
     * @deprecated transitional; use {@link #builder()} so location and check-in/out times are explicit.
     */
    @Deprecated
    public ListingAvailability(
            final Listing listing,
            final LocalDate startInclusive,
            final LocalDate endInclusive,
            final BigDecimal dayPrice,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt) {
        this(builder()
                .listing(listing)
                .startInclusive(startInclusive)
                .endInclusive(endInclusive)
                .dayPrice(dayPrice)
                .createdAt(createdAt)
                .updatedAt(updatedAt));
    }

    private ListingAvailability(final Builder b) {
        this.id = b.id;
        this.listing = b.listing;
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
        private Listing listing;
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

        public Builder listing(final Listing listing) {
            this.listing = listing;
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

        public ListingAvailability build() {
            if (car == null && listing != null) {
                car = listing.getCar();
            }
            Objects.requireNonNull(startInclusive, "startInclusive");
            Objects.requireNonNull(endInclusive, "endInclusive");
            return new ListingAvailability(this);
        }
    }

    public long getId() {
        return id;
    }

    /** Returns {@code listing}, which may be {@code null} for availability rows created without a listing. */
    public Listing getListing() {
        return listing;
    }

    /** Convenience accessor — returns {@code listing.getId()}. Throws if {@code listing_id} was not loaded. */
    public long getListingId() {
        if (listing == null) {
            throw new UnsupportedOperationException("listing_id is not set on this availability; use getCarId() instead");
        }
        return listing.getId();
    }

    public Car getCar() {
        return car;
    }

    /**
     * Dual-read: prefers the direct {@code car_id} column; falls back to {@code listing.getCar()}
     * for rows not yet backfilled in memory-only tests.
     */
    public long getCarId() {
        if (car != null) {
            return car.getId();
        }
        return listing.getCar().getId();
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
    public String toString() {
        return "ListingAvailability{"
                + "id=" + id
                + ", listingId=" + listing.getId()
                + ", startInclusive=" + startInclusive
                + ", endInclusive=" + endInclusive
                + ", dayPrice=" + Objects.toString(dayPrice)
                + ", kind=" + kind
                + ", createdAt=" + createdAt
                + ", updatedAt=" + updatedAt
                + '}';
    }
}
