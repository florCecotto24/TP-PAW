package ar.edu.itba.paw.models;

import java.time.Instant;
import java.util.List;

public final class ListingSearchCriteria {

    private final String query;
    private final List<String> transmissions;
    private final List<String> powertrains;
    private final List<String> carTypes;
    private final List<String> priceBands;
    private final Instant availabilityRangeStart;
    private final Instant availabilityRangeEndExclusive;

    public ListingSearchCriteria(
            final String query,
            final List<String> transmissions,
            final List<String> powertrains,
            final List<String> carTypes,
            final List<String> priceBands,
            final Instant availabilityRangeStart,
            final Instant availabilityRangeEndExclusive) {
        this.query = query != null && !query.isBlank() ? query.trim() : null;
        this.transmissions = transmissions == null ? List.of() : List.copyOf(transmissions);
        this.powertrains = powertrains == null ? List.of() : List.copyOf(powertrains);
        this.carTypes = carTypes == null ? List.of() : List.copyOf(carTypes);
        this.priceBands = priceBands == null ? List.of() : List.copyOf(priceBands);
        this.availabilityRangeStart = availabilityRangeStart;
        this.availabilityRangeEndExclusive = availabilityRangeEndExclusive;
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
}
