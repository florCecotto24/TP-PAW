package ar.edu.itba.paw.models.util.time;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Calendar billable-day math in {@link AppTimezone#WALL_ZONE}.
 * Shared by pricing and owner analytics so both agree on inclusive wall-day counts.
 */
public final class BillableDays {

    private BillableDays() {
    }

    /**
     * Inclusive wall-local calendar days from pickup through return, minimum 1 when the
     * interval is non-empty ({@code end} strictly after {@code start}).
     */
    public static long between(final OffsetDateTime startDate, final OffsetDateTime endDate) {
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            return 0L;
        }
        final LocalDate pickupDay = startDate.atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDate();
        final LocalDate returnDay = endDate.atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDate();
        return Math.max(1L, ChronoUnit.DAYS.between(pickupDay, returnDay.plusDays(1)));
    }
}
