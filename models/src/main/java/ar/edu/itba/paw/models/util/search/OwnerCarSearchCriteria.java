package ar.edu.itba.paw.models.util.search;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable filters and paging for an owner's "my cars" grid (status, text query, vehicle facets, price,
 * sort, refund-priority).
 *
 * Built through {@link Builder} (Effective Java 3rd ed., Item 2: <i>"Consider a builder when faced with
 * many constructor parameters"</i>): the criteria carries 15 properties — most of them optional — and the
 * telescoping-constructor and JavaBeans alternatives were both fragile (positional confusion at call sites
 * and forced mutability respectively).
 *
 * {@code
 * OwnerCarSearchCriteria.builderFor(ownerId)
 *     .page(page)
 *     .pageSize(pageSize)
 *     .carStatusFilters(statuses)
 *     .textQuery(q)
 *     .sortBy("date").sortDirection("desc")
 *     .prioritizeRefundPending(true)
 *     .build();
 * }
 */
public final class OwnerCarSearchCriteria extends BaseSearchCriteria {

    private final long ownerId;
    private final List<String> carStatusFilters;
    private final String textQuery;
    /** When set, rows for this car id are omitted (used by the counterparty profile "other active cars" grid). */
    private final Long excludeCarId;
    /**
     * When true, the DAO pre-sorts the result so cars with at least one reservation pending a refund-proof
     * upload come first (then the user-selected sort applies as a secondary criterion). Only the owner's
     * own /my-cars hub uses this — counterparty-profile grids leave it false because the visiting user
     * has no need to see the owner's refund obligations.
     */
    private final boolean prioritizeRefundPending;

    private OwnerCarSearchCriteria(final Builder b) {
        super(b.page, b.pageSize, b.carTypes, b.transmissions, b.powertrains,
                b.minPrice, b.maxPrice, b.ratingBands, b.sortBy, b.sortDirection);
        this.ownerId = b.ownerId;
        this.carStatusFilters = b.carStatusFilters == null ? List.of() : List.copyOf(b.carStatusFilters);
        this.textQuery = b.textQuery != null && !b.textQuery.isBlank() ? b.textQuery.trim() : null;
        this.excludeCarId = b.excludeCarId;
        this.prioritizeRefundPending = b.prioritizeRefundPending;
    }

    /** Entry point for the builder; {@code ownerId} is the only mandatory field. */
    public static Builder builderFor(final long ownerId) {
        return new Builder(ownerId);
    }

    public long getOwnerId() {
        return ownerId;
    }

    public List<String> getCarStatusFilters() {
        return carStatusFilters;
    }

    public String getTextQuery() {
        return textQuery;
    }

    public Long getExcludeCarId() {
        return excludeCarId;
    }

    public boolean isPrioritizeRefundPending() {
        return prioritizeRefundPending;
    }

    /**
     * Fluent builder for {@link OwnerCarSearchCriteria}. The only required property is {@code ownerId}
     * (constructor argument). Every other property defaults to "no filter" / "first page" /
     * "newest first" / "no refund prioritisation"; setting a property to {@code null} or an empty list
     * is equivalent to not calling the setter at all.
     */
    public static final class Builder {
        private final long ownerId;
        private int page;
        private int pageSize;
        private List<String> carStatusFilters;
        private String textQuery;
        private List<String> carTypes;
        private List<String> transmissions;
        private List<String> powertrains;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private List<String> ratingBands;
        private String sortBy = "date";
        private String sortDirection = "desc";
        private Long excludeCarId;
        private boolean prioritizeRefundPending;

        private Builder(final long ownerId) {
            this.ownerId = ownerId;
        }

        public Builder page(final int page) {
            this.page = page;
            return this;
        }

        public Builder pageSize(final int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder carStatusFilters(final List<String> carStatusFilters) {
            this.carStatusFilters = carStatusFilters;
            return this;
        }

        public Builder textQuery(final String textQuery) {
            this.textQuery = textQuery;
            return this;
        }

        public Builder carTypes(final List<String> carTypes) {
            this.carTypes = carTypes;
            return this;
        }

        public Builder transmissions(final List<String> transmissions) {
            this.transmissions = transmissions;
            return this;
        }

        public Builder powertrains(final List<String> powertrains) {
            this.powertrains = powertrains;
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
            this.ratingBands = ratingBands;
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

        public Builder excludeCarId(final Long excludeCarId) {
            this.excludeCarId = excludeCarId;
            return this;
        }

        public Builder prioritizeRefundPending(final boolean prioritizeRefundPending) {
            this.prioritizeRefundPending = prioritizeRefundPending;
            return this;
        }

        public OwnerCarSearchCriteria build() {
            return new OwnerCarSearchCriteria(this);
        }
    }
}
