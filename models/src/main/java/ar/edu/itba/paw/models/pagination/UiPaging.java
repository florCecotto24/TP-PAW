package ar.edu.itba.paw.models.pagination;

/**
 * Non-instantiable helper: shared UI pagination math (0-based page index, totals, clamping) for controllers and
 * {@link ar.edu.itba.paw.models.dto.Page}.
 */
public final class UiPaging {

    private UiPaging() {
    }

    /** Same rule as {@code Page#getTotalPages()} for non-empty page size. */
    public static int totalPages(final long totalItems, final int pageSize) {
        if (pageSize < 1) {
            return 1;
        }
        return totalItems == 0 ? 1 : (int) Math.ceil((double) totalItems / pageSize);
    }

    /**
     * Clamps a requested 0-based page to {@code [0, totalPages - 1]} using the same total page count as {@link #totalPages}.
     */
    public static int clampZeroBasedPage(final int page, final long totalItems, final int pageSize) {
        final int last = totalPages(totalItems, pageSize) - 1;
        return Math.min(Math.max(0, page), last);
    }
}
