package ar.edu.itba.paw.models.util;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import ar.edu.itba.paw.models.pagination.PaginationFallbackSizes;

/**
 * Immutable search filters + paging for the public car search. Prefer {@link #builder()} (Effective Java Item 2);
 * in the web app, wire sizes from Spring {@code Environment} via {@code CarService#buildSearchCriteria}.
 */
public final class CarSearchCriteria extends BaseSearchCriteria {

    private final String query;
    private final Instant availabilityRangeStart;
    private final Instant availabilityRangeEndExclusive;
    private final int dbFetchSize;
    /** When set, SQL requires published availability reaching this wall-calendar day or later. */
    private final LocalDate browseWallDate;
    /** When set, SQL excludes cars owned by this user id (public rider browse). */
    private final Long excludeOwnerUserId;
    private final List<Long> neighborhoodIds;
    /** Non-null when the rider chose flexible dates: month to search within (wall calendar). */
    private final YearMonth flexibleMonth;
    /** Minimum contiguous free-window length for flexible search; null means "any availability". */
    private final Integer flexibleDays;

    private CarSearchCriteria(final Builder b) {
        super(b.page, b.uiPageSize, b.carTypes, b.transmissions, b.powertrains,
                b.minPrice, b.maxPrice, b.ratingBands, b.sortBy, b.sortDirection);
        this.query = b.query != null && !b.query.isBlank() ? b.query.trim() : null;
        this.availabilityRangeStart = b.availabilityRangeStart;
        this.availabilityRangeEndExclusive = b.availabilityRangeEndExclusive;
        this.dbFetchSize = normalizedDbFetchSize(getPageSize(), b.dbFetchSize);
        this.browseWallDate = b.browseWallDate;
        this.excludeOwnerUserId = b.excludeOwnerUserId;
        this.neighborhoodIds = normalizeIdList(b.neighborhoodIds);
        this.flexibleMonth = b.flexibleMonth;
        this.flexibleDays = b.flexibleDays;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CarSearchCriteria} (Effective Java Item 2). Not thread-safe.
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
        private YearMonth flexibleMonth;
        private Integer flexibleDays;

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

        public Builder flexibleMonth(final YearMonth flexibleMonth) {
            this.flexibleMonth = flexibleMonth;
            return this;
        }

        public Builder flexibleDays(final Integer flexibleDays) {
            this.flexibleDays = flexibleDays;
            return this;
        }

        public CarSearchCriteria build() {
            return new CarSearchCriteria(this);
        }
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

    /** Items shown per UI page (e.g. 8). */
    public int getUiPageSize() {
        return getPageSize();
    }

    /** Rows fetched per DB query window (e.g. 24); must be ≥ {@link #getUiPageSize()}. */
    public int getDbFetchSize() {
        return dbFetchSize;
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

    /** True when the rider chose flexible dates (month-based search, no exact range). */
    public boolean isFlexibleSearch() {
        return flexibleMonth != null;
    }

    public YearMonth getFlexibleMonth() {
        return flexibleMonth;
    }

    public Integer getFlexibleDays() {
        return flexibleDays;
    }

    /**
     * Wall-calendar day count for the exact availability range (inclusive on both ends).
     * Only meaningful when {@link #hasAvailabilityRange()} is true and the search is not flexible.
     */
    public long getRangeLengthDays() {
        if (!hasAvailabilityRange()) {
            return 0L;
        }
        final LocalDate from = availabilityRangeStart.atZone(ZoneId.of("America/Argentina/Buenos_Aires")).toLocalDate();
        final LocalDate until = availabilityRangeEndExclusive.atZone(ZoneId.of("America/Argentina/Buenos_Aires")).toLocalDate().minusDays(1);
        return java.time.temporal.ChronoUnit.DAYS.between(from, until) + 1;
    }
}
