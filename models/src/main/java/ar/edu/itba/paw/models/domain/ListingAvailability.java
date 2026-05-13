package ar.edu.itba.paw.models.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/** Wall-calendar inclusive availability segment for a {@link Listing} (publish and edit forms). */
@Entity
@Table(name = "listing_availability")
public class ListingAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "listing_availability_id_seq")
    @SequenceGenerator(name = "listing_availability_id_seq", sequenceName = "listing_availability_id_seq", allocationSize = 1)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(name = "start_date", nullable = false)
    private LocalDate startInclusive;

    @Column(name = "end_date", nullable = false)
    private LocalDate endInclusive;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /* package */ ListingAvailability() {
        // For Hibernate
    }

    public ListingAvailability(
            final long id,
            final Listing listing,
            final LocalDate startInclusive,
            final LocalDate endInclusive,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt) {
        this.id = id;
        this.listing = listing;
        this.startInclusive = startInclusive;
        this.endInclusive = endInclusive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public ListingAvailability(
            final Listing listing,
            final LocalDate startInclusive,
            final LocalDate endInclusive,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt) {
        this.listing = listing;
        this.startInclusive = startInclusive;
        this.endInclusive = endInclusive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public Listing getListing() {
        return listing;
    }

    /** Convenience accessor — returns {@code listing.getId()}. */
    public long getListingId() {
        return listing.getId();
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

    @Override
    public String toString() {
        return "ListingAvailability{"
                + "id=" + id
                + ", listingId=" + listing.getId()
                + ", startInclusive=" + startInclusive
                + ", endInclusive=" + endInclusive
                + ", createdAt=" + createdAt
                + ", updatedAt=" + updatedAt
                + '}';
    }
}
