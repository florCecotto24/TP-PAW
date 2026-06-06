package ar.edu.itba.paw.policy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PresentationLimitsPolicyTest {

    private static final String COUNTERPARTY_KEY = "app.presentation.counterparty-recent-reviews-limit";
    private static final String SIMILAR_KEY = "app.presentation.car-detail-similar-listings-limit";
    private static final String OWNER_ACTIVE_LISTINGS_KEY =
            "app.presentation.counterparty-owner-active-listings-page-size";

    @Mock
    private Environment environment;

    @Test
    void testConstructorAppliesFallbacksWhenPropertiesMissing() {
        // 1.Arrange / 2.Exercise
        final PresentationLimitsPolicy policy = new PresentationLimitsPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(3, policy.getCounterpartyRecentReviewsLimit());
        Assertions.assertEquals(4, policy.getCarDetailSimilarListingsLimit());
        Assertions.assertEquals(6, policy.getCounterpartyOwnerActiveListingsPageSize());
    }

    @Test
    void testConstructorAppliesFallbacksWhenPropertiesNonPositive() {
        // 1.Arrange
        Mockito.when(environment.getProperty(COUNTERPARTY_KEY, Integer.class)).thenReturn(0);
        Mockito.when(environment.getProperty(SIMILAR_KEY, Integer.class)).thenReturn(-1);
        Mockito.when(environment.getProperty(OWNER_ACTIVE_LISTINGS_KEY, Integer.class)).thenReturn(0);

        // 2.Exercise
        final PresentationLimitsPolicy policy = new PresentationLimitsPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(3, policy.getCounterpartyRecentReviewsLimit());
        Assertions.assertEquals(4, policy.getCarDetailSimilarListingsLimit());
        Assertions.assertEquals(6, policy.getCounterpartyOwnerActiveListingsPageSize());
    }

    @Test
    void testConstructorUsesPropertyValuesWhenPositive() {
        // 1.Arrange
        Mockito.when(environment.getProperty(COUNTERPARTY_KEY, Integer.class)).thenReturn(5);
        Mockito.when(environment.getProperty(SIMILAR_KEY, Integer.class)).thenReturn(8);
        Mockito.when(environment.getProperty(OWNER_ACTIVE_LISTINGS_KEY, Integer.class)).thenReturn(9);

        // 2.Exercise
        final PresentationLimitsPolicy policy = new PresentationLimitsPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(5, policy.getCounterpartyRecentReviewsLimit());
        Assertions.assertEquals(8, policy.getCarDetailSimilarListingsLimit());
        Assertions.assertEquals(9, policy.getCounterpartyOwnerActiveListingsPageSize());
    }
}
