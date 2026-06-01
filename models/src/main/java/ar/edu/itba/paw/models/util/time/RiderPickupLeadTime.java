package ar.edu.itba.paw.models.util.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import ar.edu.itba.paw.models.domain.CarAvailability;

/**
 * First availability wall-calendar day consistent with the pickup lead-time rule
 * (configurable, e.g. {@code app.reservation.pickup-lead-hours}).
 */
public final class RiderPickupLeadTime {

    private RiderPickupLeadTime() {
    }

    /**
     * First wall-calendar day on which pickup at the listing's published time satisfies
     * {@code pickupInstant &gt; nowInstant.plus(pickupLeadHours, HOURS)}.
     *
     * @param pickupLeadHours minimum hours between "now" and pickup instant (≥ 1)
     */
    public static LocalDate minCarAvailabilityFirstDayInclusive(
            final LocalTime listingPickupWallTime,
            final ZoneId wallZone,
            final Instant nowInstant,
            final int pickupLeadHours) {
        if (pickupLeadHours < 1) {
            throw new IllegalArgumentException("pickupLeadHours must be >= 1");
        }
        final LocalTime pickup =
                listingPickupWallTime != null ? listingPickupWallTime : CarAvailability.DEFAULT_CHECK_IN_TIME;
        final Instant threshold = nowInstant.plus(pickupLeadHours, ChronoUnit.HOURS);
        LocalDate d = nowInstant.atZone(wallZone).toLocalDate();
        for (int i = 0; i < 800; i++) {
            if (ZonedDateTime.of(d, pickup, wallZone).toInstant().isAfter(threshold)) {
                return d;
            }
            d = d.plusDays(1);
        }
        return d;
    }
}
