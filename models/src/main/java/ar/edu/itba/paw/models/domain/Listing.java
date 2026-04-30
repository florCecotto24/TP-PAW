package ar.edu.itba.paw.models.domain;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

public final class Listing {

    /** Default pickup (check-in) wall time when the listing or form omits one (publish UI, JDBC fallback, availability math). */
    public static final LocalTime DEFAULT_CHECK_IN_TIME = LocalTime.of(10, 0);

    public enum Status {
        ACTIVE,
        PAUSED,
        FINISHED,
        PAUSED_DUE_TO_LACK_OF_CBU
    }

    private final long id;
    private final String title;
    private final long carId;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final Status status;
    private final BigDecimal dayPrice;
    private final String startPointStreet;
    private final String startPointNumber;
    private final String description;
    private final LocalTime checkInTime;
    private final LocalTime checkOutTime;
    private final Long neighborhoodId;
    private final BigDecimal ratingAvg;

    private Listing(final Builder b) {
        this.id = b.id;
        this.title = b.title;
        this.carId = b.carId;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
        this.status = b.status;
        this.dayPrice = b.dayPrice;
        this.startPointStreet = b.startPointStreet;
        this.startPointNumber = b.startPointNumber;
        this.description = b.description;
        this.checkInTime = b.checkInTime;
        this.checkOutTime = b.checkOutTime;
        this.neighborhoodId = b.neighborhoodId;
        this.ratingAvg = b.ratingAvg;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long id;
        private String title;
        private long carId;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private Status status;
        private BigDecimal dayPrice;
        private String startPointStreet;
        private String startPointNumber;
        private String description;
        private LocalTime checkInTime;
        private LocalTime checkOutTime;
        private Long neighborhoodId;
        private BigDecimal ratingAvg;

        public Builder id(final long id) {
            this.id = id;
            return this;
        }

        public Builder title(final String title) {
            this.title = title;
            return this;
        }

        public Builder carId(final long carId) {
            this.carId = carId;
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

        public Builder neighborhoodId(final Long neighborhoodId) {
            this.neighborhoodId = neighborhoodId;
            return this;
        }

        public Builder ratingAvg(final BigDecimal ratingAvg) {
            this.ratingAvg = ratingAvg;
            return this;
        }

        public Listing build() {
            Objects.requireNonNull(title, "title");
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

    public long getCarId() {
        return carId;
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

    public Optional<Long> getNeighborhoodId() {
        return Optional.ofNullable(neighborhoodId);
    }

    public Optional<BigDecimal> getRatingAvg() {
        return Optional.ofNullable(ratingAvg);
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
                ", carId=" + carId +
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
