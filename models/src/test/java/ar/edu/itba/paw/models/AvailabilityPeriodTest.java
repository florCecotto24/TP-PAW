package ar.edu.itba.paw.models;

import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AvailabilityPeriodTest {

    @Test
    void constructorRejectsNullStart() {
        // Arrange
        final LocalDate end = LocalDate.of(2026, 4, 10);
        // Exercise & Assert
        Assertions.assertThrows(NullPointerException.class, () -> new AvailabilityPeriod(null, end));
    }

    @Test
    void constructorRejectsNullEnd() {
        // Arrange
        final LocalDate start = LocalDate.of(2026, 4, 1);
        // Exercise & Assert
        Assertions.assertThrows(NullPointerException.class, () -> new AvailabilityPeriod(start, null));
    }

    @Test
    void isValidOrderIsTrueWhenDatesAreEqual() {
        // Arrange
        final LocalDate same = LocalDate.of(2026, 4, 1);
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
                LocalDate.of(2026, 4, 2),
                LocalDate.of(2026, 4, 1));

        // Exercise
        boolean result = period.isValidOrder();

        // Assert
        Assertions.assertFalse(result);
    }
}
