package ar.edu.itba.paw.models;

import java.time.OffsetDateTime;

public class Reservation {

    public enum Status {
        ACCEPTED, STARTED, CANCELLED, FINISHED
    }

    private final long id;
    private final long riderId;
    private final long listingId;
    private final OffsetDateTime startDate;
    private final OffsetDateTime endDate;
    private final Status status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public Reservation(
            final long id,
            final long riderId,
            final long listingId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final Status status,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt) {
        this.id = id;
        this.riderId = riderId;
        this.listingId = listingId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public long getRiderId() {
        return riderId;
    }

    public long getListingId() {
        return listingId;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public OffsetDateTime getEndDate() {
        return endDate;
    }

    public Status getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
