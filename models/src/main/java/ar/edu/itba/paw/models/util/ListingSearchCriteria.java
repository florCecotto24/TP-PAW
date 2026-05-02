package ar.edu.itba.paw.models.util;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import ar.edu.itba.paw.models.pagination.PaginationFallbackSizes;

/**
 * Immutable search filters + paging (Item 17). Prefer {@link #builder()} (Item 2); in the web app, wire sizes from
 * Spring {@code Environment} via the listing service {@code buildSearchCriteria}.
 */
public final class ListingSearchCriteria {

    private final String query;
    private final List<String> transmissions;
    private final List<String> powertrains;
    private final List<String> carTypes;
    private final BigDecimal minPrice;
    private final BigDecimal maxPrice;
    private final List<String> ratingBands;
    private final Instant availabilityRangeStart;
    private final Instant availabilityRangeEndExclusive;
    private final int page;
    private final int uiPageSize;
    private final int dbFetchSize;
    private final String sortBy;
    private final String sortDirection;
    /** When set, SQL requires published availability reaching this wall-calendar day or later. */
    private final LocalDate browseWallDate;
    /** When set, SQL excludes listings owned by this user id (public rider browse). */
    private final Long excludeOwnerUserId;
    private final List<Long> neighborhoodIds;

    private ListingSearchCriteria(final Builder b) {
        this.query = b.query != null && !b.query.isBlank() ? b.query.trim() : null;
        this.transmissions = b.transmissions;
        this.powertrains = b.powertrains;
        this.carTypes = b.carTypes;
        this.minPrice = b.minPrice;
        this.maxPrice = b.maxPrice;
        this.ratingBands = b.ratingBands;
        this.availabilityRangeStart = b.availabilityRangeStart;
        this.availabilityRangeEndExclusive = b.availabilityRangeEndExclusive;
        this.page = normalizedPage(b.page);
        final int ui = normalizedUiPageSize(b.uiPageSize);
        this.uiPageSize = ui;
        this.dbFetchSize = normalizedDbFetchSize(ui, b.dbFetchSize);
        this.sortBy = b.sortBy != null ? b.sortBy : "date";
        this.sortDirection = "asc".equalsIgnoreCase(b.sortDirection) ? "asc" : "desc";
        this.browseWallDate = b.browseWallDate;
        this.excludeOwnerUserId = b.excludeOwnerUserId;
        this.neighborhoodIds = normalizeIdList(b.neighborhoodIds);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListingSearchCriteria} (Effective Java Item 2). Not thread-safe.
     */
    public static final class Builder {

        private String query;
        private List<String> transmissions = List.of();
        private List<String> powertrains = List.of();
        private List<String> carTypes = List.of();
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private List<String> ratingBands = List.of();
        private Instant availabilityRangeStart;
        private Instant availabilityRangeEndExclusive;
        private int page;
        /** {@code 0} means use {@link PaginationFallbackSizes#UI_PAGE_SIZE} at {@link #build()}. */
        private int uiPageSize;
        /** {@code 0} means use {@link PaginationFallbackSizes#DB_FETCH_SIZE} at {@link #build()}. */
        private int dbFetchSize;
        private String sortBy = "date";
        private String sortDirection = "desc";
        private LocalDate browseWallDate;
        private Long excludeOwnerUserId;
        private List<Long> neighborhoodIds = List.of();

        public Builder query(final String query) {
            this.query = query;
            return this;
        }

        public Builder transmissions(final List<String> transmissions) {
            this.transmissions = transmissions == null ? List.of() : List.copyOf(transmissions);
            return this;
        }

        public Builder powertrains(final List<String> powertrains) {
            this.powertrains = powertrains == null ? List.of() : List.copyOf(powertrains);
            return this;
        }

        public Builder carTypes(final List<String> carTypes) {
            this.carTypes = carTypes == null ? List.of() : List.copyOf(carTypes);
            return this;
        }

        public Builder minPrice(final BigDecimal minPrice) {
            this.minPrice = minPrice;
            return this;
        }

        public Builder maxPrice(final BigDecimal maxPrice) {
            this.maxPrice = maxPrice;
            return this;
        }

        public Builder ratingBands(final List<String> ratingBands) {
            this.ratingBands = ratingBands == null ? List.of() : List.copyOf(ratingBands);
            return this;
        }

        public Builder availabilityRange(final Instant startInclusive, final Instant endExclusive) {
            this.availabilityRangeStart = startInclusive;
            this.availabilityRangeEndExclusive = endExclusive;
            return this;
        }

        public Builder page(final int page) {
            this.page = page;
            return this;
        }

        public Builder uiPageSize(final int uiPageSize) {
            this.uiPageSize = uiPageSize;
            return this;
        }

        public Builder dbFetchSize(final int dbFetchSize) {
            this.dbFetchSize = dbFetchSize;
            return this;
        }

        public Builder sortBy(final String sortBy) {
            this.sortBy = sortBy;
            return this;
        }

        public Builder sortDirection(final String sortDirection) {
            this.sortDirection = sortDirection;
            return this;
        }

        public Builder browseWallDate(final LocalDate browseWallDate) {
            this.browseWallDate = browseWallDate;
            return this;
        }

        public Builder excludeOwnerUserId(final Long excludeOwnerUserId) {
            this.excludeOwnerUserId = excludeOwnerUserId;
            return this;
        }

        public Builder neighborhoodIds(final List<Long> neighborhoodIds) {
            this.neighborhoodIds = neighborhoodIds == null ? List.of() : List.copyOf(neighborhoodIds);
            return this;
        }

        public ListingSearchCriteria build() {
            return new ListingSearchCriteria(this);
        }
    }

    private static int normalizedPage(final int page) {
        return Math.max(0, page);
    }

    private static int normalizedUiPageSize(final int uiPageSize) {
        return uiPageSize > 0 ? uiPageSize : PaginationFallbackSizes.UI_PAGE_SIZE;
    }

    /**
     * Ensures a positive DB window at least as large as the UI page (dual-layer paging invariant).
     */
    private static int normalizedDbFetchSize(final int resolvedUiPageSize, final int dbFetchSize) {
        final int base = dbFetchSize > 0 ? dbFetchSize : PaginationFallbackSizes.DB_FETCH_SIZE;
        return Math.max(resolvedUiPageSize, base);
    }

    private static List<Long> normalizeIdList(final List<Long> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        final LinkedHashSet<Long> set = new LinkedHashSet<>();
        for (final Long id : raw) {
            if (id != null && id > 0L) {
                set.add(id);
            }
        }
        return List.copyOf(new ArrayList<>(set));
    }

    public String getQuery() {
        return query;
    }

    public List<String> getTransmissions() {
        return transmissions;
    }

    public List<String> getPowertrains() {
        return powertrains;
    }

    public List<String> getCarTypes() {
        return carTypes;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public List<String> getRatingBands() {
        return ratingBands;
    }

    public Instant getAvailabilityRangeStart() {
        return availabilityRangeStart;
    }

    public Instant getAvailabilityRangeEndExclusive() {
        return availabilityRangeEndExclusive;
    }

    public boolean hasAvailabilityRange() {
        return availabilityRangeStart != null
                && availabilityRangeEndExclusive != null
                && availabilityRangeEndExclusive.isAfter(availabilityRangeStart);
    }

    public int getPage() {
        return page;
    }

    /** Items shown per UI page (e.g. 8). */
    public int getUiPageSize() {
        return uiPageSize;
    }

    /** Rows fetched per DB query window (e.g. 24); must be ≥ {@link #getUiPageSize()}. */
    public int getDbFetchSize() {
        return dbFetchSize;
    }

    public String getSortBy() {
        return sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public LocalDate getBrowseWallDate() {
        return browseWallDate;
    }

    public Long getExcludeOwnerUserId() {
        return excludeOwnerUserId;
    }

    public List<Long> getNeighborhoodIds() {
        return neighborhoodIds;
    }
}
