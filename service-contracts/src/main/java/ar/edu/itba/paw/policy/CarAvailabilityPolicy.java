package ar.edu.itba.paw.policy;

import java.time.LocalDate;
import java.util.List;

import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.util.time.AppTimezone;

/**
 * Limits listing availability windows on the publication wall calendar
 * ({@link AppTimezone#WALL_ZONE}), measured from {@code referenceWallDay} (normally "today" in that zone):
 * each period's end must not fall before {@code referenceWallDay}, and start/end must not fall after
 * {@code referenceWallDay + configuredForwardDays}.
 */
public interface CarAvailabilityPolicy {

    int getMaxAvailabilityForwardWallDays();

    void validateAvailabilityWithinPublishHorizon(LocalDate referenceWallDay, List<AvailabilityPeriod> periods);
}
