package ar.edu.itba.paw.models.util;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable filters and paging for rider or owner reservation card lists (status, vehicle facets, price, rating, sort).
 */
public final class ReservationSearchCriteria extends BaseSearchCriteria {

    private final Long ownerId;
    private final Long riderId;
    private final List<String> statusFilters;

    public ReservationSearchCriteria(
            final Long ownerId,
            final Long riderId,
            final int page,
            final int pageSize,
            final List<String> statusFilters,
            final List<String> carTypes,
            final List<String> transmissions,
            final List<String> powertrains,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final List<String> ratingBands,
            final String sortBy,
            final String sortDirection) {
        super(page, pageSize, carTypes, transmissions, powertrains, minPrice, maxPrice, ratingBands, sortBy, sortDirection);
        this.ownerId = ownerId;
        this.riderId = riderId;
        this.statusFilters = statusFilters == null ? List.of() : List.copyOf(statusFilters);
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public Long getRiderId() {
        return riderId;
    }

    public List<String> getStatusFilters() {
        return statusFilters;
    }
}
