package ar.edu.itba.paw.services.policy;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.listing.ListingValidationException;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;

/**
 * Limits how far into the future listing availability may extend on the publication wall calendar
 * ({@link AvailabilityPeriod#WALL_ZONE}), measured from {@code referenceWallDay} (normally “today” in that zone):
 * inclusive start/end dates must not fall after {@code referenceWallDay + configuredForwardDays}.
 */
@Component
public final class ListingAvailabilityPolicy {

    private final int maxAvailabilityForwardWallDays;

    @Autowired
    public ListingAvailabilityPolicy(final Environment environment) {
        this.maxAvailabilityForwardWallDays = resolveForwardWallDays(environment);
    }

    private static int resolveForwardWallDays(final Environment environment) {
        final Integer forward = environment.getProperty("app.listing.max-availability-forward-wall-days", Integer.class);
        if (forward != null && forward >= 1) {
            return forward;
        }
        final Integer legacy = environment.getProperty("app.listing.max-availability-total-days", Integer.class);
        if (legacy != null && legacy >= 1) {
            return legacy;
        }
        return 365;
    }

    public int getMaxAvailabilityForwardWallDays() {
        return maxAvailabilityForwardWallDays;
    }

    public void validateAvailabilityWithinPublishHorizon(
            final LocalDate referenceWallDay,
            final List<AvailabilityPeriod> periods) {
        Objects.requireNonNull(referenceWallDay, "referenceWallDay");
        if (periods == null || periods.isEmpty()) {
            return;
        }
        final LocalDate latestAllowedInclusive = referenceWallDay.plusDays(maxAvailabilityForwardWallDays);
        for (final AvailabilityPeriod period : periods) {
            if (period == null || !period.isValidOrder()) {
                continue;
            }
            if (period.getStartInclusive().isAfter(latestAllowedInclusive)
                    || period.getEndInclusive().isAfter(latestAllowedInclusive)) {
                throw new ListingValidationException(
                        MessageKeys.LISTING_AVAILABILITY_BEYOND_PUBLISH_HORIZON,
                        maxAvailabilityForwardWallDays);
            }
        }
    }
}
