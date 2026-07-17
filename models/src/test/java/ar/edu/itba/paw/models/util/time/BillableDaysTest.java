package ar.edu.itba.paw.models.util.time;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BillableDaysTest {

    @Test
    void testBetweenCountsInclusiveWallDays() {
        // 1.Arrange — 10:00 wall on day 1 → 10:00 wall on day 3 (UTC-3 stored as UTC).
        final OffsetDateTime start = OffsetDateTime.of(2026, 3, 1, 13, 0, 0, 0, ZoneOffset.UTC);
        final OffsetDateTime end = OffsetDateTime.of(2026, 3, 3, 13, 0, 0, 0, ZoneOffset.UTC);

        // 2.Act
        final long days = BillableDays.between(start, end);

        // 3.Assert — pickup 01, return 03 → 3 inclusive days.
        Assertions.assertEquals(3L, days);
    }

    @Test
    void testBetweenReturnsZeroWhenEndNotAfterStart() {
        // 1.Arrange
        final OffsetDateTime instant = OffsetDateTime.parse("2026-03-01T13:00:00Z");

        // 2.Act / 3.Assert
        Assertions.assertEquals(0L, BillableDays.between(instant, instant));
        Assertions.assertEquals(0L, BillableDays.between(null, instant));
    }
}
