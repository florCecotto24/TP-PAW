package ar.edu.itba.paw.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ar.edu.itba.paw.models.util.ListingSearchCriteria;

public final class ListingSearchCriteriaTest {

    @Test
    void testBuilderNormalizesQueryAndCopiesCollections() {
        // Arrange
        final List<String> transmissions = new ArrayList<>(List.of("MANUAL"));
        final List<String> powertrains = new ArrayList<>(List.of("HYBRID"));
        final List<String> carTypes = new ArrayList<>(List.of("SUV"));

        // Exercise
        final ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .query("  city center  ")
                .transmissions(transmissions)
                .powertrains(powertrains)
                .carTypes(carTypes)
                .minPrice(new BigDecimal("200"))
                .maxPrice(new BigDecimal("300"))
                .build();

        // Assert
        Assertions.assertEquals("city center", criteria.getQuery());
        Assertions.assertEquals(List.of("MANUAL"), criteria.getTransmissions());
        Assertions.assertEquals(List.of("HYBRID"), criteria.getPowertrains());
        Assertions.assertEquals(List.of("SUV"), criteria.getCarTypes());
        Assertions.assertEquals(new BigDecimal("200"), criteria.getMinPrice());
        Assertions.assertEquals(new BigDecimal("300"), criteria.getMaxPrice());

        transmissions.add("AUTOMATIC");
        powertrains.add("ELECTRIC");
        carTypes.add("SEDAN");

        Assertions.assertEquals(List.of("MANUAL"), criteria.getTransmissions());
        Assertions.assertEquals(List.of("HYBRID"), criteria.getPowertrains());
        Assertions.assertEquals(List.of("SUV"), criteria.getCarTypes());

        Assertions.assertThrows(UnsupportedOperationException.class, () -> criteria.getTransmissions().add("AUTO"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> criteria.getPowertrains().add("GASOLINE"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> criteria.getCarTypes().add("PICKUP"));
    }

    @Test
    void testBuilderTurnsBlankQueryIntoNullAndNullListsIntoEmptyLists() {

        // Exercise
        final ListingSearchCriteria criteria = ListingSearchCriteria.builder()
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

        // Exercise
        final ListingSearchCriteria valid = ListingSearchCriteria.builder()
                .availabilityRange(start, Instant.parse("2026-04-05T10:00:01Z"))
                .build();

        final ListingSearchCriteria equal = ListingSearchCriteria.builder()
                .availabilityRange(start, start)
                .build();

        final ListingSearchCriteria missingEnd = ListingSearchCriteria.builder()
                .availabilityRange(start, null)
                .build();

        // Assert
        Assertions.assertTrue(valid.hasAvailabilityRange());
        Assertions.assertFalse(equal.hasAvailabilityRange());
        Assertions.assertFalse(missingEnd.hasAvailabilityRange());
    }
}
