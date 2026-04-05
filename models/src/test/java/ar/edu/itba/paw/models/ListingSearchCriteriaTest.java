package ar.edu.itba.paw.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ListingSearchCriteriaTest {

    @Test
    void constructorNormalizesQueryAndCopiesCollections() {
        // Arrange
        final List<String> transmissions = new ArrayList<>(List.of("MANUAL"));
        final List<String> powertrains = new ArrayList<>(List.of("HYBRID"));
        final List<String> carTypes = new ArrayList<>(List.of("SUV"));
        final List<String> priceBands = new ArrayList<>(List.of("200-300"));

        // Exercise
        final ListingSearchCriteria criteria = new ListingSearchCriteria(
                "  city center  ",
                transmissions,
                powertrains,
                carTypes,
                priceBands,
                null,
                null);

        // Assert
        Assertions.assertEquals("city center", criteria.getQuery());
        Assertions.assertEquals(List.of("MANUAL"), criteria.getTransmissions());
        Assertions.assertEquals(List.of("HYBRID"), criteria.getPowertrains());
        Assertions.assertEquals(List.of("SUV"), criteria.getCarTypes());
        Assertions.assertEquals(List.of("200-300"), criteria.getPriceBands());

        transmissions.add("AUTOMATIC");
        powertrains.add("ELECTRIC");
        carTypes.add("SEDAN");
        priceBands.add("300-400");

        Assertions.assertEquals(List.of("MANUAL"), criteria.getTransmissions());
        Assertions.assertEquals(List.of("HYBRID"), criteria.getPowertrains());
        Assertions.assertEquals(List.of("SUV"), criteria.getCarTypes());
        Assertions.assertEquals(List.of("200-300"), criteria.getPriceBands());

        Assertions.assertThrows(UnsupportedOperationException.class, () -> criteria.getTransmissions().add("AUTO"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> criteria.getPowertrains().add("GASOLINE"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> criteria.getCarTypes().add("PICKUP"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> criteria.getPriceBands().add("400-500"));
    }

    @Test
    void constructorTurnsBlankQueryIntoNullAndNullListsIntoEmptyLists() {

        // Exercise
        final ListingSearchCriteria criteria = new ListingSearchCriteria(
                "   ",
                null,
                null,
                null,
                null,
                null,
                null);
        // Assert
        Assertions.assertNull(criteria.getQuery());
        Assertions.assertTrue(criteria.getTransmissions().isEmpty());
        Assertions.assertTrue(criteria.getPowertrains().isEmpty());
        Assertions.assertTrue(criteria.getCarTypes().isEmpty());
        Assertions.assertTrue(criteria.getPriceBands().isEmpty());
    }

    @Test
    void hasAvailabilityRangeIsTrueOnlyWhenEndIsAfterStart() {
        // Arrange
        final Instant start = Instant.parse("2026-04-05T10:00:00Z");

        // Exercise
        final ListingSearchCriteria valid = new ListingSearchCriteria(
                null,
                null,
                null,
                null,
                null,
                start,
                Instant.parse("2026-04-05T10:00:01Z"));

        final ListingSearchCriteria equal = new ListingSearchCriteria(
                null,
                null,
                null,
                null,
                null,
                start,
                start);

        final ListingSearchCriteria missingEnd = new ListingSearchCriteria(
                null,
                null,
                null,
                null,
                null,
                start,
                null);

        // Assert
        Assertions.assertTrue(valid.hasAvailabilityRange());
        Assertions.assertFalse(equal.hasAvailabilityRange());
        Assertions.assertFalse(missingEnd.hasAvailabilityRange());
    }
}
