package ar.edu.itba.paw.persistence.catalog;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.persistence.support.DaoIntegrationTestSupport;

class CabaNeighborhoodCatalogTest extends DaoIntegrationTestSupport {

    @Autowired
    private CabaNeighborhoodCatalog catalog;

    @Test
    void testFindAllNeighborhoodsReturnsEveryRowFromSeed() {
        // 1.Arrange
        final Long expectedRowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM neighborhoods", Long.class);

        // 2.Exercise
        final List<Neighborhood> all = catalog.findAllNeighborhoods();

        // 3.Assert
        Assertions.assertNotNull(expectedRowCount);
        Assertions.assertEquals(expectedRowCount.intValue(), all.size());
    }

    @Test
    void testFindAllNeighborhoodsReturnsCaseInsensitiveAlphabeticalOrder() {
        // 1.Arrange / 2.Exercise
        final List<Neighborhood> all = catalog.findAllNeighborhoods();

        // 3.Assert
        for (int i = 1; i < all.size(); i++) {
            final String previous = all.get(i - 1).getName();
            final String current = all.get(i).getName();
            Assertions.assertTrue(
                    String.CASE_INSENSITIVE_ORDER.compare(previous, current) <= 0,
                    "out of order at index " + i + ": '" + previous + "' should not follow '" + current + "'");
        }
    }

    @Test
    void testFindNeighborhoodByIdReturnsRowWhenPresent() {
        // 1.Arrange
        final String expectedName = jdbcTemplate.queryForObject(
                "SELECT name FROM neighborhoods WHERE id = ?", String.class, 22L);

        // 2.Exercise
        final Optional<Neighborhood> found = catalog.findNeighborhoodById(22L);

        // 3.Assert
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(22L, found.get().getId());
        Assertions.assertEquals(expectedName, found.get().getName());
    }

    @Test
    void testFindNeighborhoodByIdReturnsEmptyWhenAbsent() {
        // 1.Arrange / 2.Exercise
        final Optional<Neighborhood> found = catalog.findNeighborhoodById(9_999_999L);

        // 3.Assert
        Assertions.assertTrue(found.isEmpty());
    }
}
