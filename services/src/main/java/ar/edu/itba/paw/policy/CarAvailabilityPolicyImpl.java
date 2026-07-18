package ar.edu.itba.paw.policy;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.CarValidationException;
import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;

/**
 * Reads {@code app.listing.max-availability-forward-wall-days} (or the legacy
 * {@code app.listing.max-availability-total-days}) to back {@link CarAvailabilityPolicy}.
 */
@Component
public final class CarAvailabilityPolicyImpl implements CarAvailabilityPolicy {

    private final int maxAvailabilityForwardWallDays;

    @Autowired
    public CarAvailabilityPolicyImpl(final Environment environment) {
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

    @Override
    public int getMaxAvailabilityForwardWallDays() {
        return maxAvailabilityForwardWallDays;
    }

    @Override
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
            // Period end must not fall before the wall-calendar reference day (typically "today").
            // Create is also covered by rider-lead on start; this catches edits that shrink an
            // in-progress window so its end lands in the past.
            if (period.getEndInclusive().isBefore(referenceWallDay)) {
                throw new CarValidationException(MessageKeys.CAR_AVAILABILITY_INCLUDES_PAST_DATES);
            }
            if (period.getStartInclusive().isAfter(latestAllowedInclusive)
                    || period.getEndInclusive().isAfter(latestAllowedInclusive)) {
                throw new CarValidationException(
                        MessageKeys.CAR_AVAILABILITY_BEYOND_PUBLISH_HORIZON,
                        maxAvailabilityForwardWallDays);
            }
        }
    }
}
