package ar.edu.itba.paw.models;

import java.time.Instant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ar.edu.itba.paw.models.util.search.CarSearchCriteria;

public final class CarSearchCriteriaTest {

    @Test
    void testBuilderTurnsBlankQueryIntoNullAndNullListsIntoEmptyLists() {

        // Act
        final CarSearchCriteria criteria = CarSearchCriteria.builder()
                .query("   ")
                .transmissions(null)
                .powertrains(null)
                .carTypes(null)
                .build();

        // Assert
        Assertions.assertNull(criteria.getQuery());
        Assertions.assertTrue(criteria.getTransmissions().isEmpty());
        Assertions.assertTrue(criteria.getPowertrains().isEmpty());
        Assertions.assertTrue(criteria.getCarTypes().isEmpty());
        Assertions.assertNull(criteria.getMinPrice());
        Assertions.assertNull(criteria.getMaxPrice());
    }

    @Test
    void testHasAvailabilityRangeIsTrueOnlyWhenEndIsAfterStart() {
        // Arrange
        final Instant start = Instant.parse("2026-04-05T10:00:00Z");

        // Act
        final CarSearchCriteria valid = CarSearchCriteria.builder()
                .availabilityRange(start, Instant.parse("2026-04-05T10:00:01Z"))
                .build();

        final CarSearchCriteria equal = CarSearchCriteria.builder()
                .availabilityRange(start, start)
                .build();

        final CarSearchCriteria missingEnd = CarSearchCriteria.builder()
                .availabilityRange(start, null)
                .build();

        // Assert
        Assertions.assertTrue(valid.hasAvailabilityRange());
        Assertions.assertFalse(equal.hasAvailabilityRange());
        Assertions.assertFalse(missingEnd.hasAvailabilityRange());
    }
}
