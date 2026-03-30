package ar.edu.itba.paw.models;

import java.time.OffsetDateTime;

public final class ListingAvailability {

    private final long id;
    private final long listingId;
    private final OffsetDateTime startDate;
    private final OffsetDateTime endDate;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public ListingAvailability(
            final long id,
            final long listingId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt) {
        this.id = id;
        this.listingId = listingId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "ListingAvailability{" +
                "id=" + id +
                ", listingId=" + listingId +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
