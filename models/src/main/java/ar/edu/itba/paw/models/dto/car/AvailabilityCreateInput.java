package ar.edu.itba.paw.models.dto.car;


import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;

/**
 * Owner-side input for the create-listing / edit-availability flows. Holds the same fields the
 * controller's {@code CreateCarAvailabilityForm} and {@code CarAvailabilityEditForm} hand off
 * to the service, in a transport-neutral shape so {@link ar.edu.itba.paw.services.car.CarAvailabilityService}
 * does not depend on web-tier types.
 *
 * <p>The {@code periodPrices} list mirrors {@code periods} positionally and may be shorter or
 * contain nulls: callers fall back to {@link #pricePerDay()} for any missing slot.</p>
 */
public final class AvailabilityCreateInput {

    private final BigDecimal pricePerDay;
    private final String startPointStreet;
    private final String startPointNumber;
    private final Long neighborhoodId;
    private final LocalTime checkInTime;
    private final LocalTime checkOutTime;
    private final List<AvailabilityPeriod> periods;
    private final List<BigDecimal> periodPrices;
    private final int minimumRentalDays;

    public AvailabilityCreateInput(
            final BigDecimal pricePerDay,
            final String startPointStreet,
            final String startPointNumber,
            final Long neighborhoodId,
            final LocalTime checkInTime,
            final LocalTime checkOutTime,
            final List<AvailabilityPeriod> periods,
            final List<BigDecimal> periodPrices,
            final int minimumRentalDays) {
        this.pricePerDay = Objects.requireNonNull(pricePerDay, "pricePerDay");
        this.startPointStreet = startPointStreet;
        this.startPointNumber = startPointNumber;
        this.neighborhoodId = neighborhoodId;
        this.checkInTime = Objects.requireNonNull(checkInTime, "checkInTime");
        this.checkOutTime = Objects.requireNonNull(checkOutTime, "checkOutTime");
        this.periods = List.copyOf(Objects.requireNonNull(periods, "periods"));
        // periodPrices is positional vs periods and MAY contain nulls (callers fall back to
        // pricePerDay via effectivePriceAt). List.copyOf rejects nulls, so use a null-tolerant
        // unmodifiable copy.
        this.periodPrices = periodPrices == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(periodPrices));
        this.minimumRentalDays = minimumRentalDays;
    }

    public BigDecimal pricePerDay() { return pricePerDay; }

    public String startPointStreet() { return startPointStreet; }

    public Optional<String> startPointNumber() { return Optional.ofNullable(startPointNumber); }

    public Optional<Long> neighborhoodId() { return Optional.ofNullable(neighborhoodId); }

    public Long rawNeighborhoodId() { return neighborhoodId; }

    public String rawStartPointNumber() { return startPointNumber; }

    public LocalTime checkInTime() { return checkInTime; }

    public LocalTime checkOutTime() { return checkOutTime; }

    public List<AvailabilityPeriod> periods() { return periods; }

    public List<BigDecimal> periodPrices() { return periodPrices; }

    public int minimumRentalDays() { return minimumRentalDays; }

    /**
     * Per-period price with fallback to {@link #pricePerDay()} when the slot at {@code index}
     * is missing or {@code null}. Centralises the formula previously inlined in the controller
     * loop (and re-inlined inside {@code createCarAvailabilityPeriods}).
     */
    public BigDecimal effectivePriceAt(final int index) {
        if (index >= 0 && index < periodPrices.size()) {
            final BigDecimal slot = periodPrices.get(index);
            if (slot != null) {
                return slot;
            }
        }
        return pricePerDay;
    }
}
