package ar.edu.itba.paw.models;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public final class ListingAvailability {

    private final long id;
    private final long listingId;
    private final LocalDate startInclusive;
    private final LocalDate endInclusive;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final boolean active;

    public ListingAvailability(
            final long id,
            final long listingId,
            final LocalDate startInclusive,
            final LocalDate endInclusive,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt,
            final boolean active) {
        this.id = id;
        this.listingId = listingId;
        this.startInclusive = startInclusive;
        this.endInclusive = endInclusive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.active = active;
    }

    public long getId() {
        return id;
    }

    public long getListingId() {
        return listingId;
    }

    public LocalDate getStartInclusive() {
        return startInclusive;
    }

    public LocalDate getEndInclusive() {
        return endInclusive;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return "ListingAvailability{" +
                "id=" + id +
                ", listingId=" + listingId +
                ", startInclusive=" + startInclusive +
                ", endInclusive=" + endInclusive +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", active=" + active +
                '}';
    }
}
