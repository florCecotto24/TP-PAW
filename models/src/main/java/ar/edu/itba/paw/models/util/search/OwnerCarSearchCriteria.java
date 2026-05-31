package ar.edu.itba.paw.models.util.search;

import java.math.BigDecimal;
import java.util.List;

/** Immutable filters and paging for an owner's "my cars" grid (status, text query, vehicle facets, price, sort). */
public final class OwnerCarSearchCriteria extends BaseSearchCriteria {

    private final long ownerId;
    private final List<String> carStatusFilters;
    private final String textQuery;
    /** When set, rows for this car id are omitted (used by the counterparty profile "other active cars" grid). */
    private final Long excludeCarId;

    public OwnerCarSearchCriteria(
            final long ownerId,
            final int page,
            final int pageSize,
            final List<String> carStatusFilters,
            final String textQuery,
            final List<String> carTypes,
            final List<String> transmissions,
            final List<String> powertrains,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final List<String> ratingBands,
            final String sortBy,
            final String sortDirection,
            final Long excludeCarId) {
        super(page, pageSize, carTypes, transmissions, powertrains, minPrice, maxPrice, ratingBands, sortBy, sortDirection);
        this.ownerId = ownerId;
        this.carStatusFilters = carStatusFilters == null ? List.of() : List.copyOf(carStatusFilters);
        this.textQuery = textQuery != null && !textQuery.isBlank() ? textQuery.trim() : null;
        this.excludeCarId = excludeCarId;
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
}
