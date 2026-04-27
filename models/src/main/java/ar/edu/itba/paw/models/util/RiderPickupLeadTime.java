package ar.edu.itba.paw.models.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Cálculo de la primera fecha de disponibilidad coherente con la regla de anticipación del retiro
 * (configurable en la aplicación, p. ej. {@code app.reservation.pickup-lead-hours}).
 */
public final class RiderPickupLeadTime {

    private RiderPickupLeadTime() {
    }

    /**
     * Primer día de calendario (muro) cuyo retiro a la hora publicada del aviso ya cumple
     * {@code pickupInstant &gt; nowInstant.plus(pickupLeadHours, HOURS)}.
     *
     * @param pickupLeadHours horas mínimas entre "ahora" y el instante de retiro (≥ 1)
     */
    public static LocalDate minListingAvailabilityFirstDayInclusive(
            final LocalTime listingPickupWallTime,
            final ZoneId wallZone,
            final Instant nowInstant,
            final int pickupLeadHours) {
        if (pickupLeadHours < 1) {
            throw new IllegalArgumentException("pickupLeadHours must be >= 1");
        }
        final LocalTime pickup = listingPickupWallTime != null ? listingPickupWallTime : LocalTime.of(10, 0);
        final Instant threshold = nowInstant.plus(pickupLeadHours, ChronoUnit.HOURS);
        LocalDate d = LocalDate.now(wallZone);
        for (int i = 0; i < 800; i++) {
            if (ZonedDateTime.of(d, pickup, wallZone).toInstant().isAfter(threshold)) {
                return d;
            }
            d = d.plusDays(1);
        }
        return d;
    }
}
