package ar.edu.itba.paw.util;


import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.CarValidationException;
import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;

import ar.edu.itba.paw.services.car.CarAvailabilityServiceImpl;
/**
 * Stateless calendar math used by {@code CarAvailabilityServiceImpl}: range subtraction,
 * adjacent-day merging, identical-projection collapsing, and the wall-day pickup-lead clip.
 *
 * <p>Extracted to a utility class so the orchestrating service can stay focused on transaction
 * boundaries + DAO/policy coordination, keeping its line count under the per-service budget.</p>
 */
public final class CarAvailabilityCalendarMath {

    private CarAvailabilityCalendarMath() { }

    /**
     * Subtracts {@code [newStart, newEnd]} from {@code [oldStart, oldEnd]} and returns the
     * (up to two) remaining ranges, ordered by start date. Returns a single-element list with
     * the original range when the two ranges don't overlap at all.
     */
    public static List<DateRange> subtractDayRange(
            final LocalDate oldStart, final LocalDate oldEnd,
            final LocalDate newStart, final LocalDate newEnd) {
        final LocalDate overlapStart = oldStart.isAfter(newStart) ? oldStart : newStart;
        final LocalDate overlapEnd = oldEnd.isBefore(newEnd) ? oldEnd : newEnd;
        if (overlapStart.isAfter(overlapEnd)) {
            return List.of(new DateRange(oldStart, oldEnd));
        }
        final List<DateRange> out = new ArrayList<>(2);
        if (oldStart.isBefore(overlapStart)) {
            out.add(new DateRange(oldStart, overlapStart.minusDays(1)));
        }
        if (oldEnd.isAfter(overlapEnd)) {
            out.add(new DateRange(overlapEnd.plusDays(1), oldEnd));
        }
        return out;
    }

    /**
     * Compresses a sorted set of bookable wall days into a list of contiguous
     * {@link AvailabilityPeriod} ranges.
     */
    public static List<AvailabilityPeriod> mergeAdjacentWallDaysToPeriods(final SortedSet<LocalDate> days) {
        if (days.isEmpty()) {
            return List.of();
        }
        final List<AvailabilityPeriod> out = new ArrayList<>();
        LocalDate segStart = null;
        LocalDate prev = null;
        for (final LocalDate d : days) {
            if (segStart == null) {
                segStart = d;
                prev = d;
            } else if (d.equals(prev.plusDays(1))) {
                prev = d;
            } else {
                out.add(new AvailabilityPeriod(segStart, prev));
                segStart = d;
                prev = d;
            }
        }
        out.add(new AvailabilityPeriod(segStart, prev));
        return List.copyOf(out);
    }

    /**
     * Merges contiguous single-day {@link BookableSegmentProjection}s whose full projection
     * (price + check-in/out time + public location + neighborhood) is identical. Separate
     * segments otherwise.
     */
    public static List<BookableSegmentProjection> mergeContiguousIdenticalProjections(
            final List<BookableSegmentProjection> singleDay) {
        if (singleDay.isEmpty()) {
            return List.of();
        }
        final List<BookableSegmentProjection> out = new ArrayList<>();
        BookableSegmentProjection cur = singleDay.get(0);
        for (int i = 1; i < singleDay.size(); i++) {
            final BookableSegmentProjection next = singleDay.get(i);
            final boolean contiguous = next.getFrom().equals(cur.getTo().plusDays(1));
            final boolean sameProjection = Objects.equals(cur.getDayPrice(), next.getDayPrice())
                    && Objects.equals(cur.getCheckInTime(), next.getCheckInTime())
                    && Objects.equals(cur.getCheckOutTime(), next.getCheckOutTime())
                    && cur.getPublicLocation().equals(next.getPublicLocation())
                    && Objects.equals(cur.getNeighborhoodId(), next.getNeighborhoodId());
            if (contiguous && sameProjection) {
                cur = new BookableSegmentProjection(
                        cur.getFrom(), next.getTo(),
                        cur.getDayPrice(), cur.getCheckInTime(),
                        cur.getCheckOutTime(), cur.getPublicLocation(),
                        cur.getNeighborhoodId());
            } else {
                out.add(cur);
                cur = next;
            }
        }
        out.add(cur);
        return List.copyOf(out);
    }

    /**
     * Walks each {@link BookableSegmentProjection} forward until it finds the first day whose
     * pickup-wall-instant is strictly after {@code minPickupExclusive}; trims everything before
     * that day. Drops the segment entirely when every day fails the check.
     */
    public static List<BookableSegmentProjection> clipSegmentsByPickupLead(
            final List<BookableSegmentProjection> merged,
            final ZoneId wallZone,
            final Instant minPickupExclusive) {
        if (merged.isEmpty()) {
            return List.of();
        }
        final List<BookableSegmentProjection> clipped = new ArrayList<>();
        for (final BookableSegmentProjection seg : merged) {
            final LocalTime pickup = seg.getCheckInTime() != null
                    ? seg.getCheckInTime()
                    : CarAvailability.DEFAULT_CHECK_IN_TIME;
            LocalDate d = seg.getFrom();
            while (!d.isAfter(seg.getTo())) {
                final Instant pickupInstant = ZonedDateTime.of(d, pickup, wallZone).toInstant();
                if (pickupInstant.isAfter(minPickupExclusive)) {
                    if (d.equals(seg.getFrom())) {
                        clipped.add(seg);
                    } else {
                        clipped.add(new BookableSegmentProjection(
                                d, seg.getTo(),
                                seg.getDayPrice(), seg.getCheckInTime(),
                                seg.getCheckOutTime(), seg.getPublicLocation(),
                                seg.getNeighborhoodId()));
                    }
                    break;
                }
                d = d.plusDays(1);
            }
        }
        return List.copyOf(clipped);
    }

    /**
     * Throws {@link CarValidationException} with {@link MessageKeys#CAR_MIN_RENTAL_DAYS_EXCEEDS_PERIOD}
     * when {@code minDays} exceeds the length (inclusive) of any of the given periods.
     */
    public static void validateMinimumRentalDaysAgainstPeriods(
            final int minDays, final List<AvailabilityPeriod> periods) {
        for (final AvailabilityPeriod period : periods) {
            final long periodLength = java.time.temporal.ChronoUnit.DAYS.between(
                    period.getStartInclusive(), period.getEndInclusive()) + 1;
            if (minDays > periodLength) {
                throw new CarValidationException(
                        MessageKeys.CAR_MIN_RENTAL_DAYS_EXCEEDS_PERIOD,
                        new Object[]{minDays});
            }
        }
    }

    /** Inclusive day range carrier; lighter than {@link AvailabilityPeriod} and value-typed. */
    public static final class DateRange {
        private final LocalDate start;
        private final LocalDate end;

        public DateRange(final LocalDate start, final LocalDate end) {
            this.start = start;
            this.end = end;
        }

        public LocalDate start() {
            return start;
        }

        public LocalDate end() {
            return end;
        }
    }
}
