package ar.edu.itba.paw.models.util;

import java.math.BigDecimal;
import java.util.List;

/** Immutable filters and paging for an owner's "my listings" grid (status, text query, vehicle facets, price, sort). */
public final class OwnerListingSearchCriteria extends BaseSearchCriteria {

    private final long ownerId;
    private final List<String> listingStatusFilters;
    private final String textQuery;

    public OwnerListingSearchCriteria(
            final long ownerId,
            final int page,
            final int pageSize,
            final List<String> listingStatusFilters,
            final String textQuery,
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
        this.listingStatusFilters = listingStatusFilters == null ? List.of() : List.copyOf(listingStatusFilters);
        this.textQuery = textQuery != null && !textQuery.isBlank() ? textQuery.trim() : null;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public List<String> getListingStatusFilters() {
        return listingStatusFilters;
    }

    public String getTextQuery() {
        return textQuery;
    }
}
