package ar.edu.itba.paw.services.util;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ar.edu.itba.paw.models.domain.Neighborhood;

class NeighborhoodNameMatcherTest {

    private static final List<Neighborhood> CITY = List.of(
            new Neighborhood(1L, "Palermo"),
            new Neighborhood(2L, "Recoleta"),
            new Neighborhood(3L, "Belgrano"),
            new Neighborhood(4L, "Caballito"));

    @Test
    void testIdsMatchingFuzzyTokensReturnsEmptyForNullQuery() {
        // 1.Arrange / 2.Exercise
        final List<Long> ids = NeighborhoodNameMatcher.idsMatchingFuzzyTokens(null, CITY, 1, 3);

        // 3.Assert
        Assertions.assertTrue(ids.isEmpty());
    }

    @Test
    void testIdsMatchingFuzzyTokensReturnsEmptyForBlankQuery() {
        // 1.Arrange / 2.Exercise
        final List<Long> ids = NeighborhoodNameMatcher.idsMatchingFuzzyTokens("   ", CITY, 1, 3);

        // 3.Assert
        Assertions.assertTrue(ids.isEmpty());
    }

    @Test
    void testIdsMatchingFuzzyTokensReturnsEmptyWhenNeighborhoodsAreNullOrEmpty() {
        // 1.Arrange / 2.Exercise / 3.Assert
        Assertions.assertTrue(NeighborhoodNameMatcher.idsMatchingFuzzyTokens("palermo", null, 1, 3).isEmpty());
        Assertions.assertTrue(NeighborhoodNameMatcher.idsMatchingFuzzyTokens("palermo", List.of(), 1, 3).isEmpty());
    }

    @Test
    void testIdsMatchingFuzzyTokensReturnsEmptyWhenAllTokensTooShort() {
        // 1.Arrange / 2.Exercise
        final List<Long> ids = NeighborhoodNameMatcher.idsMatchingFuzzyTokens("ab cd", CITY, 1, 4);

        // 3.Assert
        Assertions.assertTrue(ids.isEmpty());
    }

    @Test
    void testIdsMatchingFuzzyTokensMatchesExactNameCaseInsensitive() {
        // 1.Arrange / 2.Exercise
        final List<Long> ids = NeighborhoodNameMatcher.idsMatchingFuzzyTokens("PALERMO", CITY, 0, 3);

        // 3.Assert
        Assertions.assertEquals(List.of(1L), ids);
    }

    @Test
    void testIdsMatchingFuzzyTokensTolerantOfOneEditWhenMaxDistanceIsOne() {
        // 1.Arrange / 2.Exercise
        final List<Long> ids = NeighborhoodNameMatcher.idsMatchingFuzzyTokens("palerno", CITY, 1, 3);

        // 3.Assert
        Assertions.assertEquals(List.of(1L), ids);
    }

    @Test
    void testIdsMatchingFuzzyTokensCollectsMultipleNeighborhoodsAcrossTokens() {
        // 1.Arrange / 2.Exercise
        final List<Long> ids = NeighborhoodNameMatcher.idsMatchingFuzzyTokens("palermo recoleta", CITY, 1, 3);

        // 3.Assert
        Assertions.assertTrue(ids.contains(1L));
        Assertions.assertTrue(ids.contains(2L));
        Assertions.assertEquals(2, ids.size());
    }

    @Test
    void testIdsMatchingFuzzyTokensDeduplicatesIds() {
        // 1.Arrange
        // 2.Exercise
        final List<Long> ids = NeighborhoodNameMatcher.idsMatchingFuzzyTokens("palermo palerno", CITY, 1, 3);

        // 3.Assert
        Assertions.assertEquals(List.of(1L), ids);
    }

    @Test
    void testIdsMatchingFuzzyTokensReturnsEmptyWhenNoNeighborhoodWithinDistance() {
        // 1.Arrange / 2.Exercise
        final List<Long> ids = NeighborhoodNameMatcher.idsMatchingFuzzyTokens("zorroland", CITY, 1, 3);

        // 3.Assert
        Assertions.assertTrue(ids.isEmpty());
    }
}
