package ar.edu.itba.paw.models;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;

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
    private final String startPoint;
    private final String description;
    private final LocalTime checkInTime;
    private final LocalTime checkOutTime;

    public Listing(
            final long id,
            final String title,
            final long carId,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt,
            final Status status,
            final BigDecimal dayPrice,
            final String startPoint,
            final String description,
            final LocalTime checkInTime,
            final LocalTime checkOutTime) {
        this.id = id;
        this.title = title;
        this.carId = carId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.status = status;
        this.dayPrice = dayPrice;
        this.startPoint = startPoint;
        this.description = description;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
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

    public String getStartPoint() {
        return startPoint;
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
                ", startPoint='" + startPoint + '\'' +
                ", description='" + description + '\'' +
                ", checkInTime=" + checkInTime +
                ", checkOutTime=" + checkOutTime +
                '}';
    }
}
