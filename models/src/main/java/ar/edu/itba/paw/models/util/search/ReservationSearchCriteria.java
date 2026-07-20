package ar.edu.itba.paw.models.util.search;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable filters and paging for rider or owner reservation card lists (status, vehicle facets, price, rating, sort).
 *
 * <p>Paging, vehicle facets, price range, rating bands and sort are normalized by
 * {@link BaseSearchCriteria} (as in its sibling criteria types); this subclass only adds the
 * reservation-specific scope: participant ids, car id, status filters and free-text query.</p>
 */
public final class ReservationSearchCriteria extends BaseSearchCriteria {

    private final Long ownerId;
    private final Long riderId;
    private final Long carId;
    private final List<String> statusFilters;
    private final String textQuery;

    public ReservationSearchCriteria(
            final Long ownerId,
            final Long riderId,
            final Long carId,
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
            final String sortDirection,
            final String textQuery) {
        super(page, pageSize, carTypes, transmissions, powertrains, minPrice, maxPrice, ratingBands,
                sortBy, sortDirection);
        this.ownerId = ownerId;
        this.riderId = riderId;
        this.carId = carId;
        this.statusFilters = statusFilters == null ? List.of() : List.copyOf(statusFilters);
        this.textQuery = textQuery != null && !textQuery.isBlank() ? textQuery.trim() : null;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public Long getRiderId() {
        return riderId;
    }

    public Long getCarId() {
        return carId;
    }

    public List<String> getStatusFilters() {
        return statusFilters;
    }

    public String getTextQuery() {
        return textQuery;
    }
}
