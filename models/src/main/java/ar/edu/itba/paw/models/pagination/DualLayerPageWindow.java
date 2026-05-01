package ar.edu.itba.paw.models.pagination;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable value: maps a UI page index + sizes to a SQL {@code LIMIT}/{@code OFFSET} window and an in-batch slice.
 * Used for listing browse (home, search) where the DB fetch size may exceed the UI page size.
 */
public final record DualLayerPageWindow(
        int uiPage,
        int uiPageSize,
        int dbFetchSize,
        int sqlOffset,
        int sqlLimit,
        int innerSliceStart) {

    /**
     * @param uiPage       0-based UI page
     * @param uiPageSize   items per UI page (≥ 1)
     * @param dbFetchSize  rows per SQL window (≥ {@code uiPageSize})
     */
    public static DualLayerPageWindow compute(final int uiPage, final int uiPageSize, final int dbFetchSize) {
        final int ui = Math.max(1, uiPageSize);
        int db = Math.max(1, dbFetchSize);
        if (db < ui) {
            db = ui;
        }
        final int page = Math.max(0, uiPage);
        final int globalFirst = page * ui;
        final int sqlOffset = (globalFirst / db) * db;
        final int inner = globalFirst - sqlOffset;
        return new DualLayerPageWindow(page, ui, db, sqlOffset, db, inner);
    }

    /** First global index for the current UI page (0-based in the full ordered result set). */
    public int globalFirstIndex() {
        return uiPage * uiPageSize;
    }

    /** Slice one DB batch using {@link #innerSliceStart()} and {@link #uiPageSize()}. */
    public static <T> List<T> sliceBatch(final List<T> batch, final DualLayerPageWindow w) {
        final int from = Math.min(Math.max(0, w.innerSliceStart()), batch.size());
        final int to = Math.min(from + w.uiPageSize(), batch.size());
        return new ArrayList<>(batch.subList(from, to));
    }

    /** Slice a full in-memory ordered list using {@link #globalFirstIndex()} and {@link #uiPageSize()}. */
    public static <T> List<T> sliceGlobalOrdered(final List<T> all, final DualLayerPageWindow w) {
        final int from = Math.min(w.globalFirstIndex(), all.size());
        final int to = Math.min(from + w.uiPageSize(), all.size());
        return new ArrayList<>(all.subList(from, to));
    }
}
