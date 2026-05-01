package ar.edu.itba.paw.models.pagination;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class UiPagingTest {

    @Test
    void totalPagesMatchesLegacyPageRules() {
        Assertions.assertEquals(1, UiPaging.totalPages(0, 8));
        Assertions.assertEquals(2, UiPaging.totalPages(9, 8));
        Assertions.assertEquals(1, UiPaging.totalPages(8, 8));
    }

    @Test
    void clampZeroBasedPageCapsToLast() {
        Assertions.assertEquals(1, UiPaging.clampZeroBasedPage(99, 9, 8));
        Assertions.assertEquals(0, UiPaging.clampZeroBasedPage(-3, 5, 8));
    }
}
