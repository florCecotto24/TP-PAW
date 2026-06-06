package ar.edu.itba.paw.models.dto.car;

import ar.edu.itba.paw.models.dto.car.CarPriceMarketInsight;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Neighborhood;

/**
 * Shared bundle consumed by both {@code car/createCarAvailability.jsp} (publish flow) and
 * {@code car/editCarAvailability.jsp} (per-availability edit flow) in {@code MyCarsController}.
 *
 * <p>Before {@code CarAvailabilityEditorViewService} extracted this assembly, the two controller
 * helpers {@code buildCreateListingView} and {@code buildEditAvailabilityView} duplicated:
 * publisher CBU lookup, the full set of neighborhoods, the wall-time min/max availability
 * window, the pickup-lead policy, the publisher's email, and the price-market insight. The
 * controller is left with the create/edit branching: it picks the view name and adds either
 * {@code userHasCbu} (publish flow only) or {@code availabilityId} (edit flow only).</p>
 */
public final class CarAvailabilityEditorPageModel {

    private final Car car;
    private final boolean userHasCbu;
    private final List<Neighborhood> allNeighborhoods;
    private final String publishMinAvailabilityFrom;
    private final int pickupLeadHours;
    private final int maxAvailabilityForwardWallDays;
    private final String publishMaxAvailabilityWallInclusive;
    private final String publisherEmail;
    private final CarPriceMarketInsight priceMarketInsightOrNull;

    public CarAvailabilityEditorPageModel(
            final Car car,
            final boolean userHasCbu,
            final List<Neighborhood> allNeighborhoods,
            final String publishMinAvailabilityFrom,
            final int pickupLeadHours,
            final int maxAvailabilityForwardWallDays,
            final String publishMaxAvailabilityWallInclusive,
            final String publisherEmail,
            final CarPriceMarketInsight priceMarketInsightOrNull) {
        this.car = Objects.requireNonNull(car, "car");
        this.userHasCbu = userHasCbu;
        this.allNeighborhoods = List.copyOf(allNeighborhoods);
        this.publishMinAvailabilityFrom = Objects.requireNonNull(publishMinAvailabilityFrom, "publishMinAvailabilityFrom");
        this.pickupLeadHours = pickupLeadHours;
        this.maxAvailabilityForwardWallDays = maxAvailabilityForwardWallDays;
        this.publishMaxAvailabilityWallInclusive = Objects.requireNonNull(
                publishMaxAvailabilityWallInclusive, "publishMaxAvailabilityWallInclusive");
        this.publisherEmail = Objects.requireNonNull(publisherEmail, "publisherEmail");
        this.priceMarketInsightOrNull = priceMarketInsightOrNull;
    }

    public Car getCar() { return car; }

    public boolean isUserHasCbu() { return userHasCbu; }

    public List<Neighborhood> getAllNeighborhoods() { return allNeighborhoods; }

    public String getPublishMinAvailabilityFrom() { return publishMinAvailabilityFrom; }

    public int getPickupLeadHours() { return pickupLeadHours; }

    public int getMaxAvailabilityForwardWallDays() { return maxAvailabilityForwardWallDays; }

    public String getPublishMaxAvailabilityWallInclusive() { return publishMaxAvailabilityWallInclusive; }

    public String getPublisherEmail() { return publisherEmail; }

    public Optional<CarPriceMarketInsight> getPriceMarketInsight() {
        return Optional.ofNullable(priceMarketInsightOrNull);
    }

    /**
     * Populates the {@link org.springframework.web.servlet.ModelAndView} attributes that both
     * editor views consume in common. Callers add the view-specific attributes (form, view
     * name, {@code activeTab}, {@code userHasCbu} for create, {@code availabilityId} for edit)
     * around this call.
     */
    public void populateModel(final BiConsumer<String, Object> sink) {
        sink.accept("car", car);
        sink.accept("allNeighborhoods", allNeighborhoods);
        sink.accept("publishMinAvailabilityFrom", publishMinAvailabilityFrom);
        sink.accept("pickupLeadHours", pickupLeadHours);
        sink.accept("maxAvailabilityForwardWallDays", maxAvailabilityForwardWallDays);
        sink.accept("publishMaxAvailabilityWallInclusive", publishMaxAvailabilityWallInclusive);
        sink.accept("publisherEmail", publisherEmail);
        if (priceMarketInsightOrNull != null) {
            sink.accept("priceMarketInsight", priceMarketInsightOrNull);
        }
    }
}
