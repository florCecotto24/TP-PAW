package ar.edu.itba.paw.webapp.support;

/**
 * Resolved pagination query params for a single collection request (1-based {@code page},
 * clamped {@code pageSize}). Controllers pass {@link #getZeroBasedPage()} to services/DAOs.
 *
 * Guidelines ask for a pageSize ceiling: we sanitize via clamp to
 * {@code [1, maxPageSize]} (same policy the guidelines approved as an alternative to {@code 400}).
 * OpenAPI documents {@code maximum} per collection; Bean Validation {@code @Max} on each
 * {@code @QueryParam} would duplicate the property-driven max.
 *
 * {@code page} is also capped so {@code (page - 1) * pageSize} cannot overflow a 32-bit DAO
 * {@code OFFSET} (malicious {@code ?page=30000000} → empty page, not SQL error / 500).
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
        final int rawSize = pageSizeParam != null && pageSizeParam > 0
                ? pageSizeParam
                : defaultPageSize;
        final int safeMax = Math.max(1, maxPageSize);
        final int pageSize = Math.min(Math.max(1, rawSize), safeMax);
        // Zero-based offset = (page - 1) * pageSize must fit in a signed 32-bit int.
        final int maxZeroBasedPage = Integer.MAX_VALUE / pageSize;
        final int maxOneBasedPage = maxZeroBasedPage == Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : maxZeroBasedPage + 1;
        final int safePage = Math.min(Math.max(1, page), maxOneBasedPage);
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
