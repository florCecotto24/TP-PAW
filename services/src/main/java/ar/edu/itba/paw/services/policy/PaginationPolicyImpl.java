package ar.edu.itba.paw.services.policy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.pagination.PaginationFallbackSizes;

/** Resolves {@link PaginationPolicy} from {@code app.pagination.*} properties. */
@Component
public final class PaginationPolicyImpl implements PaginationPolicy {

    private static final String UI_PAGE_SIZE = "app.pagination.ui-page-size";
    private static final String DB_FETCH_SIZE = "app.pagination.db-fetch-size";
    private static final String DEFAULT_PAGE_SIZE = "app.pagination.default-page-size";
    private static final String LISTING_PUBLIC_REVIEWS_PAGE_SIZE = "app.pagination.listing-public-reviews-page-size";

    private final int uiPageSize;
    private final int dbFetchSize;
    private final int defaultPageSize;
    private final int listingPublicReviewsPageSize;

    @Autowired
    public PaginationPolicyImpl(final Environment environment) {
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
                readPositiveInt(
                        environment,
                        LISTING_PUBLIC_REVIEWS_PAGE_SIZE,
                        PaginationFallbackSizes.LISTING_PUBLIC_REVIEWS_PAGE_SIZE);
    }

    private static int readPositiveInt(final Environment env, final String key, final int defaultValue) {
        final Integer v = env.getProperty(key, Integer.class);
        if (v == null || v < 1) {
            return defaultValue;
        }
        return v;
    }

    @Override
    public int getUiPageSize() {
        return uiPageSize;
    }

    @Override
    public int getDbFetchSize() {
        return dbFetchSize;
    }

    @Override
    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    @Override
    public int getListingPublicReviewsPageSize() {
        return listingPublicReviewsPageSize;
    }
}
