package ar.edu.itba.paw.services.policy;

import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.listing.ListingValidationException;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;

/**
 * Limits the total published availability for a listing: the sum of inclusive calendar days
 * across all rows (each period: {@code from..until} both inclusive) must not exceed the configured cap.
 */
@Component
public final class ListingAvailabilityPolicy {

    private final int maxAvailabilityTotalDays;

    @Autowired
    public ListingAvailabilityPolicy(final Environment environment) {
        this.maxAvailabilityTotalDays =
                readPositiveInt(environment, "app.listing.max-availability-total-days", 365);
    }

    private static int readPositiveInt(final Environment env, final String key, final int defaultValue) {
        final Integer v = env.getProperty(key, Integer.class);
        if (v == null || v < 1) {
            return defaultValue;
        }
        return v;
    }

    public int getMaxAvailabilityTotalDays() {
        return maxAvailabilityTotalDays;
    }

    /**
     * Sums inclusive days per valid row; total must be at most {@link #getMaxAvailabilityTotalDays()}.
     */
    public void validateAvailabilityPeriodsTotalDays(final List<AvailabilityPeriod> periods) {
        if (periods == null || periods.isEmpty()) {
            return;
        }
        long totalInclusiveDays = 0L;
        for (final AvailabilityPeriod period : periods) {
            if (period == null || !period.isValidOrder()) {
                continue;
            }
            totalInclusiveDays += ChronoUnit.DAYS.between(period.getStartInclusive(), period.getEndInclusive()) + 1;
        }
        if (totalInclusiveDays > maxAvailabilityTotalDays) {
            throw new ListingValidationException(MessageKeys.LISTING_AVAILABILITY_MAX_TOTAL_DAYS, maxAvailabilityTotalDays);
        }
    }
}
