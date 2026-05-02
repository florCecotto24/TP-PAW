package ar.edu.itba.paw.models.pagination;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class SingleLayerPageWindowTest {

    @Test
    void testComputeSetsSqlOffsetAndLimit() {
        final SingleLayerPageWindow w = SingleLayerPageWindow.compute(2, 10);
        Assertions.assertEquals(2, w.page());
        Assertions.assertEquals(10, w.pageSize());
        Assertions.assertEquals(20, w.sqlOffset());
        Assertions.assertEquals(10, w.sqlLimit());
    }
}
