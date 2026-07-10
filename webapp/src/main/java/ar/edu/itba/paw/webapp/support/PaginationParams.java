package ar.edu.itba.paw.webapp.support;

/**
 * Resolved pagination query params for a single collection request (1-based {@code page},
 * clamped {@code pageSize}). Controllers pass {@link #getZeroBasedPage()} to services/DAOs.
 */
public final class PaginationParams {

    private final int page;
    private final int pageSize;

    private PaginationParams(final int page, final int pageSize) {
        this.page = page;
        this.pageSize = pageSize;
    }

    public static PaginationParams resolve(
            final int page,
            final Integer pageSizeParam,
            final int defaultPageSize,
            final int maxPageSize) {
        final int safePage = Math.max(1, page);
        final int rawSize = pageSizeParam != null && pageSizeParam > 0
                ? pageSizeParam
                : defaultPageSize;
        final int safeMax = Math.max(1, maxPageSize);
        final int pageSize = Math.min(Math.max(1, rawSize), safeMax);
        return new PaginationParams(safePage, pageSize);
    }

    /** 1-based page number sent on the wire ({@code Link} headers, {@code page} query param). */
    public int getPage() {
        return page;
    }

    /** Effective page size after defaulting and clamping (also echoed in {@code Link}). */
    public int getPageSize() {
        return pageSize;
    }

    /** Zero-based page index for service/DAO APIs. */
    public int getZeroBasedPage() {
        return page - 1;
    }
}
