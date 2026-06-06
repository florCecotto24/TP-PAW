package ar.edu.itba.paw.models.util.search;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import ar.edu.itba.paw.models.util.time.AppTimezone;

/**
 * Immutable search filters + paging for the public car search. Prefer {@link #builder()} (Effective Java Item 2).
 *
 * <p>Only carries the UI page index and {@code uiPageSize} (controller-supplied). The DB fetch
 * window for dual-layer paging is read by the DAO from its own pagination config, so the search
 * criteria is purely about <em>what</em> to query and <em>which UI page</em>, not <em>how many
 * rows per SQL window</em>.</p>
 */
public final class CarSearchCriteria extends BaseSearchCriteria {

    private final String query;
    private final Instant availabilityRangeStart;
    private final Instant availabilityRangeEndExclusive;
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
        /** Items per UI page (controller-supplied). {@code 0} falls back to {@code 1} via base normalisation. */
        private int uiPageSize;
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
     * Wall-calendar day count for the exact availability range. Equivalent to the rental span:
     * {@code ChronoUnit.DAYS.between(from, endExclusive)} already yields that count when the
     * upper bound is exclusive (no need for the {@code -1}/{@code +1} round-trip).
     * Only meaningful when {@link #hasAvailabilityRange()} is true and the search is not flexible.
     */
    public long getRangeLengthDays() {
        if (!hasAvailabilityRange()) {
            return 0L;
        }
        final LocalDate from = availabilityRangeStart.atZone(AppTimezone.WALL_ZONE).toLocalDate();
        final LocalDate endExclusive = availabilityRangeEndExclusive.atZone(AppTimezone.WALL_ZONE).toLocalDate();
        return ChronoUnit.DAYS.between(from, endExclusive);
    }
}
