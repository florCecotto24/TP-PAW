package ar.edu.itba.paw.models.util.search;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable filters and paging for rider or owner reservation card lists (status, vehicle facets, price, rating, sort).
 *
 * <p>The {@code pageSize} is controller-supplied (read from {@code AppPaginationProperties}); a
 * non-positive value is defensively clamped to {@code 1}.</p>
 */
public final class ReservationSearchCriteria {

    private static final int MIN_PAGE_SIZE = 1;

    private final Long ownerId;
    private final Long riderId;
    private final Long carId;
    private final int page;
    private final int pageSize;
    private final List<String> statusFilters;
    private final List<String> carTypes;
    private final List<String> transmissions;
    private final List<String> powertrains;
    private final BigDecimal minPrice;
    private final BigDecimal maxPrice;
    private final List<String> ratingBands;
    private final String sortBy;
    private final String sortDirection;
    private final String textQuery;

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
            final String sortDirection,
            final String textQuery) {
        this(ownerId, riderId, null, page, pageSize, statusFilters, carTypes, transmissions,
                powertrains, minPrice, maxPrice, ratingBands, sortBy, sortDirection, textQuery);
    }

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
        this.ownerId = ownerId;
        this.riderId = riderId;
        this.carId = carId;
        this.page = Math.max(0, page);
        this.pageSize = pageSize > 0 ? pageSize : MIN_PAGE_SIZE;
        this.statusFilters = statusFilters == null ? List.of() : List.copyOf(statusFilters);
        this.carTypes = carTypes == null ? List.of() : List.copyOf(carTypes);
        this.transmissions = transmissions == null ? List.of() : List.copyOf(transmissions);
        this.powertrains = powertrains == null ? List.of() : List.copyOf(powertrains);
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.ratingBands = ratingBands == null ? List.of() : List.copyOf(ratingBands);
        this.sortBy = sortBy != null ? sortBy : "date";
        this.sortDirection = "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc";
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

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public List<String> getStatusFilters() {
        return statusFilters;
    }

    public List<String> getCarTypes() {
        return carTypes;
    }

    public List<String> getTransmissions() {
        return transmissions;
    }

    public List<String> getPowertrains() {
        return powertrains;
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

    public String getSortBy() {
        return sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public String getTextQuery() {
        return textQuery;
    }
}
