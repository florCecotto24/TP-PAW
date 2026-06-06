package ar.edu.itba.paw.webapp.config.properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * UI-layer page sizes bound from {@code app.pagination.*}. Lives in the webapp module because
 * pagination is a UI concern (clamping, page-param redirects, choosing how many cards to render).
 * The DB-side fetch window for dual-layer queries lives in {@code DbPaginationConfig} (persistence
 * module).
 *
 * Architectural rule: services do not read these sizes. Controllers inject this component and
 * pass the resolved {@code pageSize} (or {@code uiPageSize}) into the service/criteria builder.
 */
@Component
public final class AppPaginationProperties {

    private static final String UI_PAGE_SIZE = "app.pagination.ui-page-size";
    private static final String DEFAULT_PAGE_SIZE = "app.pagination.default-page-size";
    private static final String LISTING_PUBLIC_REVIEWS_PAGE_SIZE = "app.pagination.listing-public-reviews-page-size";

    private static final int FALLBACK_UI_PAGE_SIZE = 8;
    private static final int FALLBACK_LISTING_PUBLIC_REVIEWS_PAGE_SIZE = 6;

    private final int uiPageSize;
    private final int defaultPageSize;
    private final int carPublicReviewsPageSize;

    @Autowired
    public AppPaginationProperties(final Environment environment) {
        this.uiPageSize = readPositiveInt(environment, UI_PAGE_SIZE, FALLBACK_UI_PAGE_SIZE);
        final Integer legacyDefault = environment.getProperty(DEFAULT_PAGE_SIZE, Integer.class);
        this.defaultPageSize = legacyDefault != null && legacyDefault > 0 ? legacyDefault : this.uiPageSize;
        this.carPublicReviewsPageSize = readPositiveInt(
                environment, LISTING_PUBLIC_REVIEWS_PAGE_SIZE, FALLBACK_LISTING_PUBLIC_REVIEWS_PAGE_SIZE);
    }

    /** Used by search-style grids (home, /search) paired with the persistence DB fetch window. */
    public int getUiPageSize() {
        return uiPageSize;
    }

    /** Single SQL {@code LIMIT}/UI page for owner listings, reservations, favourites grids. */
    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    /** Car-detail public reviews list. */
    public int getCarPublicReviewsPageSize() {
        return carPublicReviewsPageSize;
    }

    private static int readPositiveInt(final Environment env, final String key, final int fallback) {
        final Integer v = env.getProperty(key, Integer.class);
        return v != null && v > 0 ? v : fallback;
    }
}
