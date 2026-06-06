package ar.edu.itba.paw.models.dto.car;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarAvailability;
import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.models.domain.User;

/**
 * Model attributes for the {@code car/manageCarPeriods.jsp} page.
 * Carries the owner calendar JSON, the month-filtered availability list, and the editor
 * context needed to render the inline create/edit form.
 *
 * <p>Note: {@code createCarAvailabilityForm} is NOT included here because
 * {@code CreateCarAvailabilityForm} lives in the {@code webapp} module. The controller
 * adds it to the MAV explicitly in the GET handler; in POST error paths Spring's
 * {@code @ModelAttribute} binding already puts it in the model.</p>
 */
public final class ManageCarPeriodsPageModel {

    private final Car car;
    private final User owner;
    private final String statusKey;
    private final long carImageId;
    private final String allSegmentsJson;
    private final List<CarAvailability> monthAvailabilities;
    private final YearMonth activeYearMonth;
    private final boolean canManage;
    private final boolean isFirstPeriod;
    // Editor context (for inline form)
    private final boolean userHasCbu;
    private final List<Neighborhood> allNeighborhoods;
    private final String publishMinAvailabilityFrom;
    private final int pickupLeadHours;
    private final int maxAvailabilityForwardWallDays;
    private final String publishMaxAvailabilityWallInclusive;
    private final String publisherEmail;
    private final CarPriceMarketInsight priceMarketInsight;
    private final String reservationBlockedRangesJson;
    private final Map<Long, String> reservedRangesByAvailabilityIdJson;

    public ManageCarPeriodsPageModel(
            final Car car,
            final User owner,
            final String statusKey,
            final long carImageId,
            final String allSegmentsJson,
            final List<CarAvailability> monthAvailabilities,
            final YearMonth activeYearMonth,
            final boolean canManage,
            final boolean isFirstPeriod,
            final boolean userHasCbu,
            final List<Neighborhood> allNeighborhoods,
            final String publishMinAvailabilityFrom,
            final int pickupLeadHours,
            final int maxAvailabilityForwardWallDays,
            final String publishMaxAvailabilityWallInclusive,
            final String publisherEmail,
            final CarPriceMarketInsight priceMarketInsight,
            final String reservationBlockedRangesJson,
            final Map<Long, String> reservedRangesByAvailabilityIdJson) {
        this.car = car;
        this.owner = owner;
        this.statusKey = statusKey;
        this.carImageId = carImageId;
        this.allSegmentsJson = allSegmentsJson != null ? allSegmentsJson : "[]";
        this.monthAvailabilities = List.copyOf(monthAvailabilities);
        this.activeYearMonth = activeYearMonth;
        this.canManage = canManage;
        this.isFirstPeriod = isFirstPeriod;
        this.userHasCbu = userHasCbu;
        this.allNeighborhoods = List.copyOf(allNeighborhoods);
        this.publishMinAvailabilityFrom = publishMinAvailabilityFrom;
        this.pickupLeadHours = pickupLeadHours;
        this.maxAvailabilityForwardWallDays = maxAvailabilityForwardWallDays;
        this.publishMaxAvailabilityWallInclusive = publishMaxAvailabilityWallInclusive;
        this.publisherEmail = publisherEmail;
        this.priceMarketInsight = priceMarketInsight;
        this.reservationBlockedRangesJson =
                reservationBlockedRangesJson != null ? reservationBlockedRangesJson : "[]";
        this.reservedRangesByAvailabilityIdJson =
                reservedRangesByAvailabilityIdJson != null
                        ? Map.copyOf(reservedRangesByAvailabilityIdJson)
                        : Map.of();
    }

    public boolean isUserHasCbu() {
        return userHasCbu;
    }

    public void populateModel(final BiConsumer<String, Object> putObject) {
        putObject.accept("car", car);
        putObject.accept("owner", owner);
        putObject.accept("statusKey", statusKey);
        putObject.accept("carImageId", carImageId);
        putObject.accept("allSegmentsJson", allSegmentsJson);
        putObject.accept("monthAvailabilities", monthAvailabilities);
        putObject.accept("activeYearMonth", activeYearMonth);
        putObject.accept("canManage", canManage);
        putObject.accept("isFirstPeriod", isFirstPeriod);
        putObject.accept("userHasCbu", userHasCbu);
        putObject.accept("allNeighborhoods", allNeighborhoods);
        putObject.accept("publishMinAvailabilityFrom", publishMinAvailabilityFrom);
        putObject.accept("pickupLeadHours", pickupLeadHours);
        putObject.accept("maxAvailabilityForwardWallDays", maxAvailabilityForwardWallDays);
        putObject.accept("publishMaxAvailabilityWallInclusive", publishMaxAvailabilityWallInclusive);
        putObject.accept("publisherEmail", publisherEmail);
        putObject.accept("reservationBlockedRangesJson", reservationBlockedRangesJson);
        putObject.accept("reservedRangesByAvailabilityIdJson", reservedRangesByAvailabilityIdJson);
        if (priceMarketInsight != null) {
            putObject.accept("priceMarketInsight", priceMarketInsight);
        }
    }
}
