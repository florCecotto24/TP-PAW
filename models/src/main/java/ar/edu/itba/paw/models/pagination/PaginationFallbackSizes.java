package ar.edu.itba.paw.models.pagination;

/**
 * JVM fallback sizes when {@code app.pagination.*} is unset. Keep aligned with
 * {@code application.properties} defaults and with {@code PaginationPolicy} third arguments to {@code readPositiveInt}.
 */
public final class PaginationFallbackSizes {

    private PaginationFallbackSizes() {
    }

    /** Default for {@code app.pagination.ui-page-size}. */
    public static final int UI_PAGE_SIZE = 8;

    /** Default for {@code app.pagination.db-fetch-size}. */
    public static final int DB_FETCH_SIZE = 24;

    /** Default for {@code app.pagination.listing-public-reviews-page-size} (divides {@link #DB_FETCH_SIZE}). */
    public static final int LISTING_PUBLIC_REVIEWS_PAGE_SIZE = 6;
}
