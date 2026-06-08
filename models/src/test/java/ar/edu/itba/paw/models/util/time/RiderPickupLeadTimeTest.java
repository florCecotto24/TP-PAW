package ar.edu.itba.paw.models.util.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class RiderPickupLeadTimeTest {

    @Test
    void testMinCarAvailabilityFirstDayInclusiveRejectsZeroLeadHours() {
        // 1.Arrange
        final Instant now = Instant.parse("2026-05-03T13:00:00Z");

        // 2.Act
        final Executable call = () -> RiderPickupLeadTime.minCarAvailabilityFirstDayInclusive(
                LocalTime.of(10, 0), AppTimezone.WALL_ZONE, now, 0);

        // 3.Assert
        Assertions.assertThrows(IllegalArgumentException.class, call);
    }

    @Test
    void testMinCarAvailabilityFirstDayInclusiveRejectsNegativeLeadHours() {
        // 1.Arrange
        final Instant now = Instant.parse("2026-05-03T13:00:00Z");

        // 2.Act
        final Executable call = () -> RiderPickupLeadTime.minCarAvailabilityFirstDayInclusive(
                LocalTime.of(10, 0), AppTimezone.WALL_ZONE, now, -5);

        // 3.Assert
        Assertions.assertThrows(IllegalArgumentException.class, call);
    }

    @Test
    void testMinCarAvailabilityFirstDayInclusiveReturnsTomorrowWhenTodayPickupAlreadyPast() {
        // 1.Arrange
        final Instant now = ZonedDateTime.of(LocalDate.of(2026, 5, 3), LocalTime.of(13, 0),
                AppTimezone.WALL_ZONE).toInstant();

        // 2.Act
        final LocalDate first = RiderPickupLeadTime.minCarAvailabilityFirstDayInclusive(
                LocalTime.of(10, 0), AppTimezone.WALL_ZONE, now, 1);

        // 3.Assert
        Assertions.assertEquals(LocalDate.of(2026, 5, 4), first);
    }

    @Test
    void testMinCarAvailabilityFirstDayInclusiveReturnsTodayWhenLeadStillFits() {
        // 1.Arrange
        final Instant now = ZonedDateTime.of(LocalDate.of(2026, 5, 3), LocalTime.of(6, 0),
                AppTimezone.WALL_ZONE).toInstant();

        // 2.Act
        final LocalDate first = RiderPickupLeadTime.minCarAvailabilityFirstDayInclusive(
                LocalTime.of(10, 0), AppTimezone.WALL_ZONE, now, 1);

        // 3.Assert
        Assertions.assertEquals(LocalDate.of(2026, 5, 3), first);
    }

    @Test
    void testMinCarAvailabilityFirstDayInclusiveSkipsDaysToHonourLargeLead() {
        // 1.Arrange
        final Instant now = ZonedDateTime.of(LocalDate.of(2026, 5, 3), LocalTime.of(9, 0),
                AppTimezone.WALL_ZONE).toInstant();

        // 2.Act
        final LocalDate first = RiderPickupLeadTime.minCarAvailabilityFirstDayInclusive(
                LocalTime.of(10, 0), AppTimezone.WALL_ZONE, now, 48);

        // 3.Assert
        Assertions.assertEquals(LocalDate.of(2026, 5, 5), first);
    }

    @Test
    void testMinCarAvailabilityFirstDayInclusiveUsesListingDefaultPickupWhenNull() {
        // 1.Arrange
        final Instant now = ZonedDateTime.of(LocalDate.of(2026, 5, 3), LocalTime.of(11, 0),
                AppTimezone.WALL_ZONE).toInstant();

        // 2.Act
        final LocalDate first = RiderPickupLeadTime.minCarAvailabilityFirstDayInclusive(
                null, AppTimezone.WALL_ZONE, now, 25);

        // 3.Assert
        Assertions.assertEquals(LocalDate.of(2026, 5, 5), first);
    }
}
