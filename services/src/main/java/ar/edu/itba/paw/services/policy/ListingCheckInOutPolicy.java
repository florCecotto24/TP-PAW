package ar.edu.itba.paw.services.policy;

import java.time.Duration;
import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public final class ListingCheckInOutPolicy {

    private final int minHoursBetweenCheckInAndCheckOut;

    @Autowired
    public ListingCheckInOutPolicy(final Environment environment) {
        final Integer v = environment.getProperty("app.listing.min-hours-between-checkin-checkout", Integer.class);
        this.minHoursBetweenCheckInAndCheckOut = v != null && v > 0 ? v : 6;
    }

    public int getMinHoursBetweenCheckInAndCheckOut() {
        return minHoursBetweenCheckInAndCheckOut;
    }

    /**
     * Same calendar-day window as {@code @CheckOutAfterCheckIn}: checkout strictly after check-in.
     */
    public boolean hasMinimumGap(final LocalTime checkInTime, final LocalTime checkOutTime) {
        if (checkInTime == null || checkOutTime == null) {
            return true;
        }
        if (!checkOutTime.isAfter(checkInTime)) {
            return true;
        }
        return Duration.between(checkInTime, checkOutTime).toMinutes() >= minHoursBetweenCheckInAndCheckOut * 60L;
    }
}
