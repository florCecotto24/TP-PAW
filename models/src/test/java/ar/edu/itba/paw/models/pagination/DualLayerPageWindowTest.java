package ar.edu.itba.paw.models.pagination;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class DualLayerPageWindowTest {

    @Test
    void computeAlignsSqlWindowAndInnerSlice() {
        final DualLayerPageWindow w = DualLayerPageWindow.compute(2, 8, 24);
        Assertions.assertEquals(2, w.uiPage());
        Assertions.assertEquals(8, w.uiPageSize());
        Assertions.assertEquals(24, w.dbFetchSize());
        Assertions.assertEquals(16, w.globalFirstIndex());
        Assertions.assertEquals(0, w.sqlOffset());
        Assertions.assertEquals(24, w.sqlLimit());
        Assertions.assertEquals(16, w.innerSliceStart());
    }

    @Test
    void computeAdvancesSqlOffsetAcrossBatchBoundary() {
        final DualLayerPageWindow w = DualLayerPageWindow.compute(3, 8, 24);
        Assertions.assertEquals(24, w.sqlOffset());
        Assertions.assertEquals(0, w.innerSliceStart());
    }

    @Test
    void sliceBatchReturnsUiSizedWindow() {
        final List<Integer> batch = IntStream.range(0, 24).boxed().toList();
        final DualLayerPageWindow w = DualLayerPageWindow.compute(1, 8, 24);
        final List<Integer> slice = DualLayerPageWindow.sliceBatch(batch, w);
        Assertions.assertEquals(List.of(8, 9, 10, 11, 12, 13, 14, 15), slice);
    }

    @Test
    void sliceGlobalOrderedUsesFullListIndex() {
        final List<String> all = List.of("a", "b", "c", "d", "e");
        final DualLayerPageWindow w = DualLayerPageWindow.compute(1, 2, 24);
        Assertions.assertEquals(List.of("c", "d"), DualLayerPageWindow.sliceGlobalOrdered(all, w));
    }

    @Test
    void dbSmallerThanUiIsExpanded() {
        final DualLayerPageWindow w = DualLayerPageWindow.compute(0, 8, 4);
        Assertions.assertEquals(8, w.dbFetchSize());
    }
}
