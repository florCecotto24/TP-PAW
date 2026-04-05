package ar.edu.itba.paw.models;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AvailabilityPeriodTest {

    @Test
    void constructorRejectsNullStart() {
        // Arrange
        final LocalDateTime end = LocalDateTime.of(2026, 4, 1, 10, 30);
        // Exercise & Assert
        Assertions.assertThrows(NullPointerException.class, () -> new AvailabilityPeriod(null, end));
    }

    @Test
    void constructorRejectsNullEnd() {
        // Arrange
        final LocalDateTime start = LocalDateTime.of(2026, 4, 1, 10, 30);
        // Exercise & Assert
        Assertions.assertThrows(NullPointerException.class, () -> new AvailabilityPeriod(start, null));
    }

    @Test
    void isValidOrderIsTrueWhenDatesAreEqual() {
        // Arrange
        final LocalDateTime same = LocalDateTime.of(2026, 4, 1, 10, 30);
        final AvailabilityPeriod period = new AvailabilityPeriod(same, same);
        // Exercise
        boolean result = period.isValidOrder();
        // Assert
        Assertions.assertTrue(result);
    }

    @Test
    void isValidOrderIsFalseWhenEndIsBeforeStart() {
        // Arrange
        final AvailabilityPeriod period = new AvailabilityPeriod(
                LocalDateTime.of(2026, 4, 1, 11, 0),
                LocalDateTime.of(2026, 4, 1, 10, 59));
        // Exercise
        boolean result = period.isValidOrder();
        // Assert
        Assertions.assertFalse(result);
    }

    @Test
    void startInstantUtcUsesBuenosAiresWallClockZone() {
        // Arrange
        final AvailabilityPeriod period = new AvailabilityPeriod(
                LocalDateTime.of(2026, 4, 1, 10, 30),
                LocalDateTime.of(2026, 4, 1, 10, 45));
        // Exercise
        final OffsetDateTime result = period.startInstantUtc();
        // Assert
        Assertions.assertEquals(OffsetDateTime.parse("2026-04-01T13:30:00Z"), result);
    }

    @Test
    void endExclusiveInstantUtcAddsOneMinuteBeforeConverting() {
        // Arrange
        final AvailabilityPeriod period = new AvailabilityPeriod(
                LocalDateTime.of(2026, 4, 1, 10, 30),
                LocalDateTime.of(2026, 4, 1, 10, 45));
        // Exercise
        final OffsetDateTime result = period.endExclusiveInstantUtc();
        // Assert
        Assertions.assertEquals(OffsetDateTime.parse("2026-04-01T13:46:00Z"), result);
    }
}
