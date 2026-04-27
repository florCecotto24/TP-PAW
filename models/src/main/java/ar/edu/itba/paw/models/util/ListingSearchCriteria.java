package ar.edu.itba.paw.models.util;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class ListingSearchCriteria {

    private final String query;
    private final List<String> transmissions;
    private final List<String> powertrains;
    private final List<String> carTypes;
    private final List<String> priceBands;
    private final Instant availabilityRangeStart;
    private final Instant availabilityRangeEndExclusive;
    private final int page;
    private final int pageSize;
    private final String sortBy;
    private final String sortDirection;
    /** When set, SQL requires published availability reaching this wall-calendar day or later. */
    private final LocalDate browseWallDate;
    /** When set, SQL excludes listings owned by this user id (public rider browse). */
    private final Long excludeOwnerUserId;
    private final List<Long> neighborhoodIds;

    public ListingSearchCriteria(
            final String query,
            final List<String> transmissions,
            final List<String> powertrains,
            final List<String> carTypes,
            final List<String> priceBands,
            final Instant availabilityRangeStart,
            final Instant availabilityRangeEndExclusive) {
        this(query, transmissions, powertrains, carTypes, priceBands,
                availabilityRangeStart, availabilityRangeEndExclusive, 0, 8, "date", "desc", null, null, List.of());
    }

    public ListingSearchCriteria(
            final String query,
            final List<String> transmissions,
            final List<String> powertrains,
            final List<String> carTypes,
            final List<String> priceBands,
            final Instant availabilityRangeStart,
            final Instant availabilityRangeEndExclusive,
            final int page,
            final int pageSize,
            final String sortBy,
            final String sortDirection) {
        this(query, transmissions, powertrains, carTypes, priceBands,
                availabilityRangeStart, availabilityRangeEndExclusive, page, pageSize, sortBy, sortDirection, null, null,
                List.of());
    }

    public ListingSearchCriteria(
            final String query,
            final List<String> transmissions,
            final List<String> powertrains,
            final List<String> carTypes,
            final List<String> priceBands,
            final Instant availabilityRangeStart,
            final Instant availabilityRangeEndExclusive,
            final int page,
            final int pageSize,
            final String sortBy,
            final String sortDirection,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId) {
        this(query, transmissions, powertrains, carTypes, priceBands,
                availabilityRangeStart, availabilityRangeEndExclusive, page, pageSize, sortBy, sortDirection,
                browseWallDate, excludeOwnerUserId, List.of());
    }

    public ListingSearchCriteria(
            final String query,
            final List<String> transmissions,
            final List<String> powertrains,
            final List<String> carTypes,
            final List<String> priceBands,
            final Instant availabilityRangeStart,
            final Instant availabilityRangeEndExclusive,
            final int page,
            final int pageSize,
            final String sortBy,
            final String sortDirection,
            final LocalDate browseWallDate,
            final Long excludeOwnerUserId,
            final List<Long> neighborhoodIds) {
        this.query = query != null && !query.isBlank() ? query.trim() : null;
        this.transmissions = transmissions == null ? List.of() : List.copyOf(transmissions);
        this.powertrains = powertrains == null ? List.of() : List.copyOf(powertrains);
        this.carTypes = carTypes == null ? List.of() : List.copyOf(carTypes);
        this.priceBands = priceBands == null ? List.of() : List.copyOf(priceBands);
        this.availabilityRangeStart = availabilityRangeStart;
        this.availabilityRangeEndExclusive = availabilityRangeEndExclusive;
        this.page = Math.max(0, page);
        this.pageSize = pageSize > 0 ? pageSize : 8;
        this.sortBy = sortBy != null ? sortBy : "date";
        this.sortDirection = "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc";
        this.browseWallDate = browseWallDate;
        this.excludeOwnerUserId = excludeOwnerUserId;
        this.neighborhoodIds = normalizeIdList(neighborhoodIds);
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

    public List<String> getTransmissions() {
        return transmissions;
    }

    public List<String> getPowertrains() {
        return powertrains;
    }

    public List<String> getCarTypes() {
        return carTypes;
    }

    public List<String> getPriceBands() {
        return priceBands;
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

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public String getSortBy() {
        return sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
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
}
