package ar.edu.itba.paw.models.util.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.ListingAvailability;

class BookableWallAvailabilityCalendarTest {

    private static AvailabilityPeriod period(final int startDay, final int endDay) {
        return new AvailabilityPeriod(
                LocalDate.of(2026, 5, startDay),
                LocalDate.of(2026, 5, endDay));
    }

    @Test
    void testMergeAdjacentPeriodsReturnsEmptyListForEmptyInput() {
        // 1.Arrange
        final List<AvailabilityPeriod> raw = List.of();

        // 2.Exercise
        final List<AvailabilityPeriod> merged = BookableWallAvailabilityCalendar.mergeAdjacentPeriods(raw);

        // 3.Assert
        Assertions.assertTrue(merged.isEmpty());
    }

    @Test
    void testMergeAdjacentPeriodsKeepsSinglePeriod() {
        // 1.Arrange
        final AvailabilityPeriod only = period(3, 5);

        // 2.Exercise
        final List<AvailabilityPeriod> merged = BookableWallAvailabilityCalendar.mergeAdjacentPeriods(List.of(only));

        // 3.Assert
        Assertions.assertEquals(1, merged.size());
        Assertions.assertEquals(only.getStartInclusive(), merged.get(0).getStartInclusive());
        Assertions.assertEquals(only.getEndInclusive(), merged.get(0).getEndInclusive());
    }

    @Test
    void testMergeAdjacentPeriodsCollapsesAdjacentRanges() {
        // 1.Arrange
        final AvailabilityPeriod a = period(3, 5);
        final AvailabilityPeriod b = period(6, 8);

        // 2.Exercise
        final List<AvailabilityPeriod> merged = BookableWallAvailabilityCalendar.mergeAdjacentPeriods(List.of(a, b));

        // 3.Assert
        Assertions.assertEquals(1, merged.size());
        Assertions.assertEquals(LocalDate.of(2026, 5, 3), merged.get(0).getStartInclusive());
        Assertions.assertEquals(LocalDate.of(2026, 5, 8), merged.get(0).getEndInclusive());
    }

    @Test
    void testMergeAdjacentPeriodsCollapsesOverlappingRanges() {
        // 1.Arrange
        final AvailabilityPeriod a = period(3, 8);
        final AvailabilityPeriod b = period(5, 10);

        // 2.Exercise
        final List<AvailabilityPeriod> merged = BookableWallAvailabilityCalendar.mergeAdjacentPeriods(List.of(a, b));

        // 3.Assert
        Assertions.assertEquals(1, merged.size());
        Assertions.assertEquals(LocalDate.of(2026, 5, 3), merged.get(0).getStartInclusive());
        Assertions.assertEquals(LocalDate.of(2026, 5, 10), merged.get(0).getEndInclusive());
    }

    @Test
    void testMergeAdjacentPeriodsKeepsContainedRangeAsSinglePeriod() {
        // 1.Arrange
        final AvailabilityPeriod a = period(3, 20);
        final AvailabilityPeriod b = period(5, 7);

        // 2.Exercise
        final List<AvailabilityPeriod> merged = BookableWallAvailabilityCalendar.mergeAdjacentPeriods(List.of(a, b));

        // 3.Assert
        Assertions.assertEquals(1, merged.size());
        Assertions.assertEquals(LocalDate.of(2026, 5, 3), merged.get(0).getStartInclusive());
        Assertions.assertEquals(LocalDate.of(2026, 5, 20), merged.get(0).getEndInclusive());
    }

    @Test
    void testMergeAdjacentPeriodsKeepsGappedRangesSeparate() {
        // 1.Arrange
        final AvailabilityPeriod a = period(3, 5);
        final AvailabilityPeriod b = period(8, 10);

        // 2.Exercise
        final List<AvailabilityPeriod> merged = BookableWallAvailabilityCalendar.mergeAdjacentPeriods(List.of(a, b));

        // 3.Assert
        Assertions.assertEquals(2, merged.size());
        Assertions.assertEquals(LocalDate.of(2026, 5, 3), merged.get(0).getStartInclusive());
        Assertions.assertEquals(LocalDate.of(2026, 5, 5), merged.get(0).getEndInclusive());
        Assertions.assertEquals(LocalDate.of(2026, 5, 8), merged.get(1).getStartInclusive());
        Assertions.assertEquals(LocalDate.of(2026, 5, 10), merged.get(1).getEndInclusive());
    }

    @Test
    void testMergeAdjacentPeriodsSortsUnsortedInput() {
        // 1.Arrange
        final AvailabilityPeriod a = period(10, 12);
        final AvailabilityPeriod b = period(3, 5);
        final AvailabilityPeriod c = period(6, 8);

        // 2.Exercise
        final List<AvailabilityPeriod> merged = BookableWallAvailabilityCalendar.mergeAdjacentPeriods(List.of(a, b, c));

        // 3.Assert
        Assertions.assertEquals(2, merged.size());
        Assertions.assertEquals(LocalDate.of(2026, 5, 3), merged.get(0).getStartInclusive());
        Assertions.assertEquals(LocalDate.of(2026, 5, 8), merged.get(0).getEndInclusive());
        Assertions.assertEquals(LocalDate.of(2026, 5, 10), merged.get(1).getStartInclusive());
        Assertions.assertEquals(LocalDate.of(2026, 5, 12), merged.get(1).getEndInclusive());
    }

    @Test
    void testClipPeriodsToMinPickupInstantReturnsEmptyForEmptyMerged() {
        // 1.Arrange
        final Instant cut = Instant.parse("2026-05-03T13:00:00Z");

        // 2.Exercise
        final List<AvailabilityPeriod> clipped = BookableWallAvailabilityCalendar
                .clipPeriodsToMinPickupInstant(List.of(), LocalTime.of(10, 0), AppTimezone.WALL_ZONE, cut);

        // 3.Assert
        Assertions.assertTrue(clipped.isEmpty());
    }

    @Test
    void testClipPeriodsToMinPickupInstantKeepsPeriodAfterCut() {
        // 1.Arrange
        final AvailabilityPeriod seg = period(3, 10);
        final Instant cut = ZonedDateTime.of(LocalDate.of(2026, 5, 1), LocalTime.of(0, 0),
                AppTimezone.WALL_ZONE).toInstant();

        // 2.Exercise
        final List<AvailabilityPeriod> clipped = BookableWallAvailabilityCalendar
                .clipPeriodsToMinPickupInstant(List.of(seg), LocalTime.of(10, 0),
                        AppTimezone.WALL_ZONE, cut);

        // 3.Assert
        Assertions.assertEquals(1, clipped.size());
        Assertions.assertEquals(LocalDate.of(2026, 5, 3), clipped.get(0).getStartInclusive());
        Assertions.assertEquals(LocalDate.of(2026, 5, 10), clipped.get(0).getEndInclusive());
    }

    @Test
    void testClipPeriodsToMinPickupInstantTrimsStartWhenCutFallsInsideSegment() {
        // 1.Arrange
        final AvailabilityPeriod seg = period(3, 10);
        final Instant cut = ZonedDateTime.of(LocalDate.of(2026, 5, 5), LocalTime.of(12, 0),
                AppTimezone.WALL_ZONE).toInstant();

        // 2.Exercise
        final List<AvailabilityPeriod> clipped = BookableWallAvailabilityCalendar
                .clipPeriodsToMinPickupInstant(List.of(seg), LocalTime.of(10, 0),
                        AppTimezone.WALL_ZONE, cut);

        // 3.Assert
        Assertions.assertEquals(1, clipped.size());
        Assertions.assertEquals(LocalDate.of(2026, 5, 6), clipped.get(0).getStartInclusive());
        Assertions.assertEquals(LocalDate.of(2026, 5, 10), clipped.get(0).getEndInclusive());
    }

    @Test
    void testClipPeriodsToMinPickupInstantDropsPeriodEntirelyBeforeCut() {
        // 1.Arrange
        final AvailabilityPeriod seg = period(3, 10);
        final Instant cut = ZonedDateTime.of(LocalDate.of(2026, 5, 11), LocalTime.of(23, 59),
                AppTimezone.WALL_ZONE).toInstant();

        // 2.Exercise
        final List<AvailabilityPeriod> clipped = BookableWallAvailabilityCalendar
                .clipPeriodsToMinPickupInstant(List.of(seg), LocalTime.of(10, 0),
                        AppTimezone.WALL_ZONE, cut);

        // 3.Assert
        Assertions.assertTrue(clipped.isEmpty());
    }

    @Test
    void testClipPeriodsToMinPickupInstantUsesListingDefaultPickupTimeWhenNull() {
        // 1.Arrange
        final AvailabilityPeriod seg = period(3, 5);
        final Instant cut = ZonedDateTime.of(LocalDate.of(2026, 5, 4), LocalTime.of(9, 0),
                AppTimezone.WALL_ZONE).toInstant();

        // 2.Exercise
        final List<AvailabilityPeriod> clipped = BookableWallAvailabilityCalendar
                .clipPeriodsToMinPickupInstant(List.of(seg), null,
                        AppTimezone.WALL_ZONE, cut);

        // 3.Assert
        Assertions.assertEquals(1, clipped.size());
        Assertions.assertEquals(LocalDate.of(2026, 5, 4), clipped.get(0).getStartInclusive());
        Assertions.assertEquals(ListingAvailability.DEFAULT_CHECK_IN_TIME, LocalTime.of(10, 0),
                "guard: this test relies on the default check-in time staying at 10:00");
    }
}
