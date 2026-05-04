package ar.edu.itba.paw.services.policy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Caps for teaser-style UI snippets (not SQL pagination windows).
 */
@Component
public final class PresentationLimitsPolicy {

    private static final String COUNTERPARTY_RECENT_REVIEWS =
            "app.presentation.counterparty-recent-reviews-limit";
    private static final String CAR_DETAIL_SIMILAR_LISTINGS =
            "app.presentation.car-detail-similar-listings-limit";
    private static final String COUNTERPARTY_OWNER_ACTIVE_LISTINGS_PAGE =
            "app.presentation.counterparty-owner-active-listings-page-size";

    /** When {@link #COUNTERPARTY_RECENT_REVIEWS} is unset or invalid. */
    private static final int FALLBACK_COUNTERPARTY_RECENT_REVIEWS_LIMIT = 3;
    /** When {@link #CAR_DETAIL_SIMILAR_LISTINGS} is unset or invalid. */
    private static final int FALLBACK_CAR_DETAIL_SIMILAR_LISTINGS_LIMIT = 4;
    /** When {@link #COUNTERPARTY_OWNER_ACTIVE_LISTINGS_PAGE} is unset or invalid. */
    private static final int FALLBACK_COUNTERPARTY_OWNER_ACTIVE_LISTINGS_PAGE_SIZE = 6;

    private final int counterpartyRecentReviewsLimit;
    private final int carDetailSimilarListingsLimit;
    private final int counterpartyOwnerActiveListingsPageSize;

    @Autowired
    public PresentationLimitsPolicy(final Environment environment) {
        this.counterpartyRecentReviewsLimit =
                readPositiveInt(environment, COUNTERPARTY_RECENT_REVIEWS, FALLBACK_COUNTERPARTY_RECENT_REVIEWS_LIMIT);
        this.carDetailSimilarListingsLimit =
                readPositiveInt(
                        environment,
                        CAR_DETAIL_SIMILAR_LISTINGS,
                        FALLBACK_CAR_DETAIL_SIMILAR_LISTINGS_LIMIT);
        this.counterpartyOwnerActiveListingsPageSize =
                readPositiveInt(
                        environment,
                        COUNTERPARTY_OWNER_ACTIVE_LISTINGS_PAGE,
                        FALLBACK_COUNTERPARTY_OWNER_ACTIVE_LISTINGS_PAGE_SIZE);
    }

    private static int readPositiveInt(final Environment env, final String key, final int defaultValue) {
        final Integer v = env.getProperty(key, Integer.class);
        if (v == null || v < 1) {
            return defaultValue;
        }
        return v;
    }

    /**
     * How many comment-bearing reviews to show for counterparty profile sidebars (/car-detail and /my-reservations).
     */
    public int getCounterpartyRecentReviewsLimit() {
        return counterpartyRecentReviewsLimit;
    }

    /** Max similar listing cards on {@code /car-detail}. */
    public int getCarDetailSimilarListingsLimit() {
        return carDetailSimilarListingsLimit;
    }

    /** Page size for counterparty profile “other active listings” grid and load-more batches. */
    public int getCounterpartyOwnerActiveListingsPageSize() {
        return counterpartyOwnerActiveListingsPageSize;
    }
}
