package ar.edu.itba.paw.services.policy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Resolves {@link PresentationLimitsPolicy} from {@code app.presentation.*} properties. */
@Component
public final class PresentationLimitsPolicyImpl implements PresentationLimitsPolicy {

    private static final String COUNTERPARTY_RECENT_REVIEWS =
            "app.presentation.counterparty-recent-reviews-limit";
    private static final String CAR_DETAIL_SIMILAR_LISTINGS =
            "app.presentation.car-detail-similar-listings-limit";
    private static final String COUNTERPARTY_OWNER_ACTIVE_LISTINGS_PAGE =
            "app.presentation.counterparty-owner-active-listings-page-size";

    private static final int FALLBACK_COUNTERPARTY_RECENT_REVIEWS_LIMIT = 3;
    private static final int FALLBACK_CAR_DETAIL_SIMILAR_LISTINGS_LIMIT = 4;
    private static final int FALLBACK_COUNTERPARTY_OWNER_ACTIVE_LISTINGS_PAGE_SIZE = 6;

    private final int counterpartyRecentReviewsLimit;
    private final int carDetailSimilarListingsLimit;
    private final int counterpartyOwnerActiveListingsPageSize;

    @Autowired
    public PresentationLimitsPolicyImpl(final Environment environment) {
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

    @Override
    public int getCounterpartyRecentReviewsLimit() {
        return counterpartyRecentReviewsLimit;
    }

    @Override
    public int getCarDetailSimilarListingsLimit() {
        return carDetailSimilarListingsLimit;
    }

    @Override
    public int getCounterpartyOwnerActiveListingsPageSize() {
        return counterpartyOwnerActiveListingsPageSize;
    }
}
