package ar.edu.itba.paw.models.util;

import ar.edu.itba.paw.models.pagination.PaginationFallbackSizes;

import java.math.BigDecimal;
import java.util.List;

/** Common immutable filters, paging, and sort shared by all search criteria types. */
public abstract class BaseSearchCriteria {

    private final int page;
    private final int pageSize;
    private final List<String> carTypes;
    private final List<String> transmissions;
    private final List<String> powertrains;
    private final BigDecimal minPrice;
    private final BigDecimal maxPrice;
    private final List<String> ratingBands;
    private final String sortBy;
    private final String sortDirection;

    protected BaseSearchCriteria(
            final int page,
            final int pageSize,
            final List<String> carTypes,
            final List<String> transmissions,
            final List<String> powertrains,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final List<String> ratingBands,
            final String sortBy,
            final String sortDirection) {
        this.page = Math.max(0, page);
        this.pageSize = pageSize > 0 ? pageSize : PaginationFallbackSizes.UI_PAGE_SIZE;
        this.carTypes = carTypes == null ? List.of() : List.copyOf(carTypes);
        this.transmissions = transmissions == null ? List.of() : List.copyOf(transmissions);
        this.powertrains = powertrains == null ? List.of() : List.copyOf(powertrains);
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.ratingBands = ratingBands == null ? List.of() : List.copyOf(ratingBands);
        this.sortBy = sortBy != null ? sortBy : "date";
        this.sortDirection = "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc";
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
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
}
