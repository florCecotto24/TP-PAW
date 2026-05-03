package ar.edu.itba.paw.models.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MyHubSortSanitizerTest {

    private static final String DEFAULT_SORT = "date,desc";

    @Test
    void testSanitizeReturnsDefaultWhenNull() {
        // 1.Arrange / 2.Exercise
        final String result = MyHubSortSanitizer.sanitize(null, DEFAULT_SORT);

        // 3.Assert
        Assertions.assertEquals(DEFAULT_SORT, result);
    }

    @Test
    void testSanitizeReturnsDefaultWhenBlank() {
        // 1.Arrange / 2.Exercise
        final String result = MyHubSortSanitizer.sanitize("   ", DEFAULT_SORT);

        // 3.Assert
        Assertions.assertEquals(DEFAULT_SORT, result);
    }

    @Test
    void testSanitizeReturnsDefaultWhenNotInWhitelist() {
        // 1.Arrange
        final String evil = "name,desc";

        // 2.Exercise
        final String result = MyHubSortSanitizer.sanitize(evil, DEFAULT_SORT);

        // 3.Assert
        Assertions.assertEquals(DEFAULT_SORT, result);
    }

    @Test
    void testSanitizeReturnsValueWhenWhitelisted() {
        // 1.Arrange / 2.Exercise
        final String dateAsc = MyHubSortSanitizer.sanitize("date,asc", DEFAULT_SORT);
        final String priceAsc = MyHubSortSanitizer.sanitize("price,asc", DEFAULT_SORT);
        final String priceDesc = MyHubSortSanitizer.sanitize("price,desc", DEFAULT_SORT);
        final String ratingAsc = MyHubSortSanitizer.sanitize("rating,asc", DEFAULT_SORT);
        final String ratingDesc = MyHubSortSanitizer.sanitize("rating,desc", DEFAULT_SORT);
        final String dateDesc = MyHubSortSanitizer.sanitize("date,desc", DEFAULT_SORT);

        // 3.Assert
        Assertions.assertEquals("date,asc", dateAsc);
        Assertions.assertEquals("price,asc", priceAsc);
        Assertions.assertEquals("price,desc", priceDesc);
        Assertions.assertEquals("rating,asc", ratingAsc);
        Assertions.assertEquals("rating,desc", ratingDesc);
        Assertions.assertEquals("date,desc", dateDesc);
    }

    @Test
    void testSanitizeIsCaseSensitiveAndRejectsUppercaseValue() {
        // 1.Arrange / 2.Exercise
        final String result = MyHubSortSanitizer.sanitize("DATE,DESC", DEFAULT_SORT);

        // 3.Assert
        Assertions.assertEquals(DEFAULT_SORT, result);
    }
}
