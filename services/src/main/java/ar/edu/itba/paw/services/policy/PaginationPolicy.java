package ar.edu.itba.paw.services.policy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.pagination.PaginationFallbackSizes;

/**
 * Listing grids: DB fetch window vs UI page size (e.g. fetch 24 rows per query, show 8 per view).
 * Other screens use {@link #getDefaultPageSize()} as the SQL/UI page size (single-layer pagination).
 */
@Component
public final class PaginationPolicy {

    private static final String UI_PAGE_SIZE = "app.pagination.ui-page-size";
    private static final String DB_FETCH_SIZE = "app.pagination.db-fetch-size";
    private static final String DEFAULT_PAGE_SIZE = "app.pagination.default-page-size";
    private static final String LISTING_PUBLIC_REVIEWS_PAGE_SIZE = "app.pagination.listing-public-reviews-page-size";

    private final int uiPageSize;
    private final int dbFetchSize;
    private final int defaultPageSize;
    private final int listingPublicReviewsPageSize;

    @Autowired
    public PaginationPolicy(final Environment environment) {
        int ui = readPositiveInt(environment, UI_PAGE_SIZE, PaginationFallbackSizes.UI_PAGE_SIZE);
        int db = readPositiveInt(environment, DB_FETCH_SIZE, PaginationFallbackSizes.DB_FETCH_SIZE);
        if (db < ui) {
            db = ui;
        }
        this.uiPageSize = ui;
        this.dbFetchSize = db;
        final Integer legacyDefault = environment.getProperty(DEFAULT_PAGE_SIZE, Integer.class);
        this.defaultPageSize = legacyDefault != null && legacyDefault > 0
                ? legacyDefault
                : ui;
        this.listingPublicReviewsPageSize =
                readPositiveInt(environment, LISTING_PUBLIC_REVIEWS_PAGE_SIZE, 5);
    }

    private static int readPositiveInt(final Environment env, final String key, final int defaultValue) {
        final Integer v = env.getProperty(key, Integer.class);
        if (v == null || v < 1) {
            return defaultValue;
        }
        return v;
    }

    /** Items per UI page for home search-style grids (paired with {@link #getDbFetchSize()} for SQL windows). */
    public int getUiPageSize() {
        return uiPageSize;
    }

    /** Max rows fetched per query when using dual-layer (UI) + (DB) pagination for listing cards. */
    public int getDbFetchSize() {
        return dbFetchSize;
    }

    /**
     * Single page size for owner/reservation grids and legacy callers (SQL {@code LIMIT} equals UI page).
     */
    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    /** Listing detail → public reviews block. */
    public int getListingPublicReviewsPageSize() {
        return listingPublicReviewsPageSize;
    }
}
