package ar.edu.itba.paw.services.policy;

/**
 * Listing grids: DB fetch window vs UI page size (e.g. fetch 24 rows per query, show 8 per view).
 * Other screens use {@link #getDefaultPageSize()} as the SQL/UI page size (single-layer pagination).
 */
public interface PaginationPolicy {

    /** Items per UI page for home search-style grids (paired with {@link #getDbFetchSize()} for SQL windows). */
    int getUiPageSize();

    /** Max rows fetched per query when using dual-layer (UI) + (DB) pagination for listing cards. */
    int getDbFetchSize();

    /**
     * Single page size for owner/reservation grids and legacy callers (SQL {@code LIMIT} equals UI page).
     */
    int getDefaultPageSize();

    /** Listing detail → public reviews block. */
    int getListingPublicReviewsPageSize();
}
