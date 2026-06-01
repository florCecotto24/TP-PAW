package ar.edu.itba.paw.models.util.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.CarAvailability;

/**
 * Wall-calendar availability segments for rider booking UIs: merge adjacent ranges and clip to the minimum
 * pickup instant rule (pickup at published wall time must be after {@code minPickupExclusive}).
 */
public final class BookableWallAvailabilityCalendar {

    private BookableWallAvailabilityCalendar() {
    }

    public static List<AvailabilityPeriod> mergeAdjacentPeriods(final List<AvailabilityPeriod> raw) {
        if (raw.isEmpty()) {
            return List.of();
        }
        final List<AvailabilityPeriod> sorted = new ArrayList<>(raw);
        sorted.sort(Comparator.comparing(AvailabilityPeriod::getStartInclusive));
        final List<AvailabilityPeriod> out = new ArrayList<>();
        AvailabilityPeriod cur = sorted.get(0);
        for (int i = 1; i < sorted.size(); i++) {
            final AvailabilityPeriod next = sorted.get(i);
            if (!next.getStartInclusive().isAfter(cur.getEndInclusive().plusDays(1))) {
                final LocalDate newEnd = cur.getEndInclusive().isBefore(next.getEndInclusive())
                        ? next.getEndInclusive()
                        : cur.getEndInclusive();
                cur = new AvailabilityPeriod(cur.getStartInclusive(), newEnd);
            } else {
                out.add(cur);
                cur = next;
            }
        }
        out.add(cur);
        return List.copyOf(out);
    }

    /**
     * Trims the start of each segment so the first selectable pickup day matches the same rule as reservation pickup
     * lead time: pickup instant {@code > minPickupExclusive}.
     *
     * @param pickupWallTime published pickup time in {@code wallZone}; if null, {@link CarAvailability#DEFAULT_CHECK_IN_TIME}
     */
    public static List<AvailabilityPeriod> clipPeriodsToMinPickupInstant(
            final List<AvailabilityPeriod> merged,
            final LocalTime pickupWallTime,
            final ZoneId wallZone,
            final Instant minPickupExclusive) {
        if (merged.isEmpty()) {
            return List.of();
        }
        final LocalTime pickup = pickupWallTime != null ? pickupWallTime : CarAvailability.DEFAULT_CHECK_IN_TIME;
        final List<AvailabilityPeriod> clipped = new ArrayList<>();
        for (final AvailabilityPeriod seg : merged) {
            LocalDate d = seg.getStartInclusive();
            while (!d.isAfter(seg.getEndInclusive())) {
                final Instant pickupInstant = ZonedDateTime.of(d, pickup, wallZone).toInstant();
                if (pickupInstant.isAfter(minPickupExclusive)) {
                    clipped.add(new AvailabilityPeriod(d, seg.getEndInclusive()));
                    break;
                }
                d = d.plusDays(1);
            }
        }
        return mergeAdjacentPeriods(clipped);
    }
}
