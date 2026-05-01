package ar.edu.itba.paw.models.pagination;

/**
 * Immutable value: one page size for both SQL and UI ({@code OFFSET = page * pageSize}, {@code LIMIT = pageSize}).
 */
public final record SingleLayerPageWindow(int page, int pageSize, int sqlOffset, int sqlLimit) {

    /**
     * @param page     0-based page index (negative becomes 0)
     * @param pageSize rows per page (≤ 0 becomes 1)
     */
    public static SingleLayerPageWindow compute(final int page, final int pageSize) {
        final int ps = Math.max(1, pageSize);
        final int p = Math.max(0, page);
        return new SingleLayerPageWindow(p, ps, p * ps, ps);
    }
}
