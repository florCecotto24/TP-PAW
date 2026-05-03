package ar.edu.itba.paw.models.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;

class WallDateTimeParsingTest {

    @Test
    void testParseWallLocalDateTimeToUtcMapsArgentinaWallTime() {
        // 1.Arrange
        final String input = "2026-05-03T10:00";
        final OffsetDateTime expected = LocalDateTime.of(2026, 5, 3, 10, 0)
                .atZone(AvailabilityPeriod.WALL_ZONE)
                .toInstant()
                .atOffset(ZoneOffset.UTC);

        // 2.Exercise
        final OffsetDateTime actual = WallDateTimeParsing.parseWallLocalDateTimeToUtc(input);

        // 3.Assert
        Assertions.assertEquals(expected, actual);
        Assertions.assertEquals(ZoneOffset.UTC, actual.getOffset());
    }

    @Test
    void testParseWallLocalDateTimeToUtcTrimsWhitespace() {
        // 1.Arrange
        final String input = "  2026-05-03T10:00  ";

        // 2.Exercise
        final OffsetDateTime actual = WallDateTimeParsing.parseWallLocalDateTimeToUtc(input);

        // 3.Assert
        Assertions.assertEquals(2026, actual.getYear());
        Assertions.assertEquals(5, actual.getMonthValue());
        Assertions.assertEquals(3, actual.getDayOfMonth());
    }

    @Test
    void testParseWallLocalDateTimeToUtcThrowsOnInvalidValue() {
        // 1.Arrange
        final String input = "not-a-date";

        // 2.Exercise / 3.Assert
        Assertions.assertThrows(DateTimeParseException.class,
                () -> WallDateTimeParsing.parseWallLocalDateTimeToUtc(input));
    }

    @Test
    void testParseSearchFilterRangeStartInstantReturnsNullWhenBlank() {
        // 1.Arrange
        final String blank = "   ";

        // 2.Exercise
        final Instant fromBlank = WallDateTimeParsing.parseSearchFilterRangeStartInstant(blank);
        final Instant fromNull = WallDateTimeParsing.parseSearchFilterRangeStartInstant(null);

        // 3.Assert
        Assertions.assertNull(fromBlank);
        Assertions.assertNull(fromNull);
    }

    @Test
    void testParseSearchFilterRangeStartInstantReturnsNullWhenUnparseable() {
        // 1.Arrange
        final String garbage = "2026/05/03";

        // 2.Exercise
        final Instant actual = WallDateTimeParsing.parseSearchFilterRangeStartInstant(garbage);

        // 3.Assert
        Assertions.assertNull(actual);
    }

    @Test
    void testParseSearchFilterRangeStartInstantUsesStartOfDayForDateOnly() {
        // 1.Arrange
        final String input = "2026-05-03";
        final Instant expected = LocalDate.of(2026, 5, 3)
                .atStartOfDay(AvailabilityPeriod.WALL_ZONE)
                .toInstant();

        // 2.Exercise
        final Instant actual = WallDateTimeParsing.parseSearchFilterRangeStartInstant(input);

        // 3.Assert
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testParseSearchFilterRangeStartInstantUsesExactMinuteForDateTime() {
        // 1.Arrange
        final String input = "2026-05-03T13:45";
        final Instant expected = LocalDateTime.of(2026, 5, 3, 13, 45)
                .atZone(AvailabilityPeriod.WALL_ZONE)
                .toInstant();

        // 2.Exercise
        final Instant actual = WallDateTimeParsing.parseSearchFilterRangeStartInstant(input);

        // 3.Assert
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testParseSearchFilterRangeEndExclusiveInstantUsesNextDayForDateOnly() {
        // 1.Arrange
        final String input = "2026-05-03";
        final Instant expected = LocalDate.of(2026, 5, 4)
                .atStartOfDay(AvailabilityPeriod.WALL_ZONE)
                .toInstant();

        // 2.Exercise
        final Instant actual = WallDateTimeParsing.parseSearchFilterRangeEndExclusiveInstant(input);

        // 3.Assert
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testParseSearchFilterRangeEndExclusiveInstantUsesNextMinuteForDateTime() {
        // 1.Arrange
        final String input = "2026-05-03T13:45";
        final Instant expected = LocalDateTime.of(2026, 5, 3, 13, 46)
                .atZone(AvailabilityPeriod.WALL_ZONE)
                .toInstant();

        // 2.Exercise
        final Instant actual = WallDateTimeParsing.parseSearchFilterRangeEndExclusiveInstant(input);

        // 3.Assert
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testParseSearchFilterRangeEndExclusiveInstantReturnsNullForBadInput() {
        // 1.Arrange
        final String garbage = "yesterday";

        // 2.Exercise
        final Instant fromGarbage = WallDateTimeParsing.parseSearchFilterRangeEndExclusiveInstant(garbage);
        final Instant fromNull = WallDateTimeParsing.parseSearchFilterRangeEndExclusiveInstant(null);
        final Instant fromBlank = WallDateTimeParsing.parseSearchFilterRangeEndExclusiveInstant("");

        // 3.Assert
        Assertions.assertNull(fromGarbage);
        Assertions.assertNull(fromNull);
        Assertions.assertNull(fromBlank);
    }
}
