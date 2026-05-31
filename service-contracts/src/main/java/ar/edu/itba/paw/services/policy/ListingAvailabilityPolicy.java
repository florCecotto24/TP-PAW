package ar.edu.itba.paw.services.policy;

import java.time.LocalDate;
import java.util.List;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;

/**
 * Limits how far into the future listing availability may extend on the publication wall calendar
 * ({@link AvailabilityPeriod#WALL_ZONE}), measured from {@code referenceWallDay} (normally "today" in that zone):
 * inclusive start/end dates must not fall after {@code referenceWallDay + configuredForwardDays}.
 */
public interface ListingAvailabilityPolicy {

    int getMaxAvailabilityForwardWallDays();

    void validateAvailabilityWithinPublishHorizon(LocalDate referenceWallDay, List<AvailabilityPeriod> periods);
}
