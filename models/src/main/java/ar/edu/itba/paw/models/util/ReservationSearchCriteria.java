package ar.edu.itba.paw.models.util;

import ar.edu.itba.paw.models.pagination.PaginationFallbackSizes;

import java.util.List;

public final class ReservationSearchCriteria {

    private final Long ownerId;
    private final Long riderId;
    private final int page;
    private final int pageSize;
    private final List<String> statusFilters;
    private final List<String> carTypes;
    private final List<String> transmissions;
    private final List<String> powertrains;
    private final List<String> priceBands;
    private final String sortBy;
    private final String sortDirection;

    public ReservationSearchCriteria(
            final Long ownerId,
            final Long riderId,
            final int page,
            final int pageSize,
            final List<String> statusFilters,
            final List<String> carTypes,
            final List<String> transmissions,
            final List<String> powertrains,
            final List<String> priceBands,
            final String sortBy,
            final String sortDirection) {
        this.ownerId = ownerId;
        this.riderId = riderId;
        this.page = Math.max(0, page);
        this.pageSize = pageSize > 0 ? pageSize : PaginationFallbackSizes.UI_PAGE_SIZE;
        this.statusFilters = statusFilters == null ? List.of() : List.copyOf(statusFilters);
        this.carTypes = carTypes == null ? List.of() : List.copyOf(carTypes);
        this.transmissions = transmissions == null ? List.of() : List.copyOf(transmissions);
        this.powertrains = powertrains == null ? List.of() : List.copyOf(powertrains);
        this.priceBands = priceBands == null ? List.of() : List.copyOf(priceBands);
        this.sortBy = sortBy != null ? sortBy : "date";
        this.sortDirection = "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc";
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public Long getRiderId() {
        return riderId;
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

    public List<String> getPriceBands() {
        return priceBands;
    }

    public String getSortBy() {
        return sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }
}
