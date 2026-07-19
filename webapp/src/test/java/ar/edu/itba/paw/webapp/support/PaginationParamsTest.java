package ar.edu.itba.paw.webapp.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PaginationParamsTest {

    @Test
    void testResolveClampsPageSizeToMax() {
        // 1.Arrange / 2.Act
        final PaginationParams params = PaginationParams.resolve(1, 10_000, 8, 100);

        // 3.Assert
        assertEquals(1, params.getPage());
        assertEquals(100, params.getPageSize());
        assertEquals(0, params.getZeroBasedPage());
    }

    @Test
    void testResolveClampsHugePageToAvoidIntOverflow() {
        // 1.Arrange / 2.Act — page*pageSize would overflow int without clamp
        final PaginationParams params = PaginationParams.resolve(30_000_000, 100, 8, 100);

        // 3.Assert
        assertEquals(Integer.MAX_VALUE / 100 + 1, params.getPage());
        assertEquals(100, params.getPageSize());
        final long offset = (long) params.getZeroBasedPage() * params.getPageSize();
        assertTrue(offset <= Integer.MAX_VALUE);
    }

    @Test
    void testResolveDefaultsInvalidPageToOne() {
        // 1.Arrange / 2.Act
        final PaginationParams params = PaginationParams.resolve(-5, null, 8, 100);

        // 3.Assert
        assertEquals(1, params.getPage());
        assertEquals(8, params.getPageSize());
    }
}
