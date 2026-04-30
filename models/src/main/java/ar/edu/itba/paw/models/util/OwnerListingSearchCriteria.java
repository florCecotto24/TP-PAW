package ar.edu.itba.paw.models.util;

import java.util.List;
import java.util.Objects;

public final class OwnerListingSearchCriteria {

    private final long ownerId;
    private final int page;
    private final int pageSize;
    private final List<String> listingStatusFilters;
    private final String textQuery;
    private final List<String> carTypes;
    private final List<String> transmissions;
    private final List<String> powertrains;
    private final List<String> priceBands;
    private final String sortBy;
    private final String sortDirection;

    public OwnerListingSearchCriteria(
            final long ownerId,
            final int page,
            final int pageSize,
            final List<String> listingStatusFilters,
            final String textQuery,
            final List<String> carTypes,
            final List<String> transmissions,
            final List<String> powertrains,
            final List<String> priceBands,
            final String sortBy,
            final String sortDirection) {
        this.ownerId = ownerId;
        this.page = Math.max(0, page);
        this.pageSize = pageSize > 0 ? pageSize : 8;
        this.listingStatusFilters = listingStatusFilters == null ? List.of() : List.copyOf(listingStatusFilters);
        this.textQuery = textQuery != null && !textQuery.isBlank() ? textQuery.trim() : null;
        this.carTypes = carTypes == null ? List.of() : List.copyOf(carTypes);
        this.transmissions = transmissions == null ? List.of() : List.copyOf(transmissions);
        this.powertrains = powertrains == null ? List.of() : List.copyOf(powertrains);
        this.priceBands = priceBands == null ? List.of() : List.copyOf(priceBands);
        this.sortBy = sortBy != null ? sortBy : "date";
        this.sortDirection = "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc";
    }

    public long getOwnerId() {
        return ownerId;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public List<String> getListingStatusFilters() {
        return listingStatusFilters;
    }

    public String getTextQuery() {
        return textQuery;
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
