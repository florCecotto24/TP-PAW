package ar.edu.itba.paw.models;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;

public class Listing {

    public enum Status {
        ACTIVE, PAUSED, FINISHED
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

    public Listing(
            final long id,
            final String title,
            final long carId,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt,
            final Status status,
            final BigDecimal dayPrice,
            final String startPointStreet,
            final String description,
            final LocalTime checkInTime,
            final LocalTime checkOutTime) {
        this(id, title, carId, createdAt, updatedAt, status, dayPrice, startPointStreet, null, description, checkInTime, checkOutTime,
                null);
    }

    public Listing(
            final long id,
            final String title,
            final long carId,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt,
            final Status status,
            final BigDecimal dayPrice,
            final String startPointStreet,
            final String startPointNumber,
            final String description,
            final LocalTime checkInTime,
            final LocalTime checkOutTime,
            final Long neighborhoodId) {
        this.id = id;
        this.title = title;
        this.carId = carId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.status = status;
        this.dayPrice = dayPrice;
        this.startPointStreet = startPointStreet;
        this.startPointNumber = startPointNumber;
        this.description = description;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.neighborhoodId = neighborhoodId;
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

    /** Calle + altura (trim), para mail o vista con datos completos. */
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
                '}';
    }
}
