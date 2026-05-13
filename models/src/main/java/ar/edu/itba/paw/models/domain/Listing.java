package ar.edu.itba.paw.models.domain;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Published rental offer: links to {@link Car}, daily price, handover address, wall check-in/out times, status, and optional neighborhood and rating cache.
 */
@Entity
@Table(name = "listings")
public class Listing {

    /** Default pickup (check-in) wall time when the listing or form omits one (publish UI, JDBC fallback, availability math). */
    public static final LocalTime DEFAULT_CHECK_IN_TIME = LocalTime.of(10, 0);

    public enum Status {
        ACTIVE,
        PAUSED,
        FINISHED,
        PAUSED_DUE_TO_LACK_OF_CBU
    }

    @Converter
    public static class StatusConverter implements AttributeConverter<Status, String> {
        @Override
        public String convertToDatabaseColumn(final Status attribute) {
            return attribute == null ? null : attribute.name().toLowerCase();
        }
        @Override
        public Status convertToEntityAttribute(final String dbData) {
            return dbData == null ? null : Status.valueOf(dbData.toUpperCase());
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "listings_id_seq")
    @SequenceGenerator(name = "listings_id_seq", sequenceName = "listings_id_seq", allocationSize = 1)
    private long id;

    @Column(nullable = false, length = 105)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Convert(converter = StatusConverter.class)
    @Column(nullable = false, length = 40)
    private Status status;

    @Column(name = "day_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal dayPrice;

    @Column(name = "start_point_street", nullable = false, length = 50)
    private String startPointStreet;

    @Column(name = "start_point_number", length = 10)
    private String startPointNumber;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(name = "check_in_time", nullable = false)
    private LocalTime checkInTime;

    @Column(name = "check_out_time", nullable = false)
    private LocalTime checkOutTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "neighborhood_id", nullable = true)
    private Neighborhood neighborhood;

    @Column(name = "rating_avg", precision = 4, scale = 2)
    private BigDecimal ratingAvg;

    @OneToMany(mappedBy = "listing", fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ListingAvailability> availabilities = new ArrayList<>();

    @OneToMany(mappedBy = "listing", fetch = FetchType.LAZY)
    private List<Reservation> reservations = new ArrayList<>();

    /* package */ Listing() {
        // For Hibernate
    }

    private Listing(final Builder b) {
        this.id = b.id;
        this.title = b.title;
        this.car = b.car;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
        this.status = b.status;
        this.dayPrice = b.dayPrice;
        this.startPointStreet = b.startPointStreet;
        this.startPointNumber = b.startPointNumber;
        this.description = b.description;
        this.checkInTime = b.checkInTime;
        this.checkOutTime = b.checkOutTime;
        this.neighborhood = b.neighborhood;
        this.ratingAvg = b.ratingAvg;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long id;
        private String title;
        private Car car;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private Status status;
        private BigDecimal dayPrice;
        private String startPointStreet;
        private String startPointNumber;
        private String description;
        private LocalTime checkInTime;
        private LocalTime checkOutTime;
        private Neighborhood neighborhood;
        private BigDecimal ratingAvg;

        public Builder id(final long id) {
            this.id = id;
            return this;
        }

        public Builder title(final String title) {
            this.title = title;
            return this;
        }

        public Builder car(final Car car) {
            this.car = car;
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

        public Builder status(final Status status) {
            this.status = status;
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

        public Builder description(final String description) {
            this.description = description;
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

        public Builder neighborhood(final Neighborhood neighborhood) {
            this.neighborhood = neighborhood;
            return this;
        }

        public Builder ratingAvg(final BigDecimal ratingAvg) {
            this.ratingAvg = ratingAvg;
            return this;
        }

        public Listing build() {
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(car, "car");
            Objects.requireNonNull(createdAt, "createdAt");
            Objects.requireNonNull(updatedAt, "updatedAt");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(dayPrice, "dayPrice");
            Objects.requireNonNull(checkInTime, "checkInTime");
            Objects.requireNonNull(checkOutTime, "checkOutTime");
            return new Listing(this);
        }
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Car getCar() {
        return car;
    }

    /** Convenience accessor — returns {@code car.getId()}. */
    public long getCarId() {
        return car.getId();
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Status getStatus() {
        return status;
    }

    public BigDecimal getDayPrice() {
        return dayPrice;
    }

    public String getStartPointStreet() {
        return startPointStreet;
    }

    public Optional<String> getStartPointNumber() {
        return Optional.ofNullable(startPointNumber).filter(s -> !s.isBlank());
    }

    public String getDescription() {
        return description;
    }

    public LocalTime getCheckInTime() {
        return checkInTime;
    }

    public LocalTime getCheckOutTime() {
        return checkOutTime;
    }

    public Neighborhood getNeighborhood() {
        return neighborhood;
    }

    /** Convenience accessor — returns the neighborhood id, or empty if no neighborhood is set. */
    public Optional<Long> getNeighborhoodId() {
        return neighborhood == null ? Optional.empty() : Optional.of(neighborhood.getId());
    }

    public Optional<BigDecimal> getRatingAvg() {
        return Optional.ofNullable(ratingAvg);
    }

    public void setDayPrice(final BigDecimal dayPrice) {
        this.dayPrice = dayPrice;
    }

    public void setStartPointStreet(final String startPointStreet) {
        this.startPointStreet = startPointStreet;
    }

    public void setStartPointNumber(final String startPointNumber) {
        this.startPointNumber = startPointNumber;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setCheckInTime(final LocalTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public void setCheckOutTime(final LocalTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public void setNeighborhood(final Neighborhood neighborhood) {
        this.neighborhood = neighborhood;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public void setUpdatedAt(final OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setRatingAvg(final BigDecimal ratingAvg) {
        this.ratingAvg = ratingAvg;
    }

    public List<ListingAvailability> getAvailabilities() {
        return availabilities;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    /** Street + number (trimmed), for email or full-detail views. */
    public Optional<String> getFullPickupStreetLine() {
        final String s = startPointStreet == null ? "" : startPointStreet.trim();
        final String n = startPointNumber == null ? "" : startPointNumber.trim();
        if (s.isEmpty() && n.isEmpty()) {
            return Optional.empty();
        }
        if (n.isEmpty()) {
            return Optional.of(s);
        }
        if (s.isEmpty()) {
            return Optional.of(n);
        }
        return Optional.of(s + " " + n);
    }

    @Override
    public String toString() {
        return "Listing{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", carId=" + car.getId() +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", status=" + status +
                ", dayPrice=" + dayPrice +
                ", startPointStreet='" + startPointStreet + '\'' +
                ", description='" + description + '\'' +
                ", checkInTime=" + checkInTime +
                ", checkOutTime=" + checkOutTime +
                ", ratingAvg=" + Objects.toString(ratingAvg) +
                '}';
    }
}
