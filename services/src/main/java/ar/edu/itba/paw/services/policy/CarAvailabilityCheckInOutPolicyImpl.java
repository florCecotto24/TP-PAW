package ar.edu.itba.paw.services.policy;

import java.time.Duration;
import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Reads {@code app.listing.min-hours-between-checkin-checkout} to back {@link CarAvailabilityCheckInOutPolicy}. */
@Component
public final class CarAvailabilityCheckInOutPolicyImpl implements CarAvailabilityCheckInOutPolicy {

    private final int minHoursBetweenCheckInAndCheckOut;

    @Autowired
    public CarAvailabilityCheckInOutPolicyImpl(final Environment environment) {
        final Integer v = environment.getProperty("app.listing.min-hours-between-checkin-checkout", Integer.class);
        this.minHoursBetweenCheckInAndCheckOut = v != null && v > 0 ? v : 6;
    }

    @Override
    public int getMinHoursBetweenCheckInAndCheckOut() {
        return minHoursBetweenCheckInAndCheckOut;
    }

    @Override
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
