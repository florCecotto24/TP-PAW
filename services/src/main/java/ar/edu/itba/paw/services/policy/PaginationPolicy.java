package ar.edu.itba.paw.services.policy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Web listing-grid and related pagination defaults from {@code application.properties}.
 */
@Component
public final class PaginationPolicy {

    private static final String DEFAULT_PAGE_SIZE = "app.pagination.default-page-size";
    private static final String LISTING_PUBLIC_REVIEWS_PAGE_SIZE = "app.pagination.listing-public-reviews-page-size";

    private final int defaultPageSize;
    private final int listingPublicReviewsPageSize;

    @Autowired
    public PaginationPolicy(final Environment environment) {
        this.defaultPageSize = readPositiveInt(environment, DEFAULT_PAGE_SIZE, 8);
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

    /** Default page size for listing cards (home carousel, owner lists, rider reservations grid, counterpart listings, …). */
    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    /** Listing detail → public reviews block. */
    public int getListingPublicReviewsPageSize() {
        return listingPublicReviewsPageSize;
    }
}
