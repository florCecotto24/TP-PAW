package ar.edu.itba.paw.services.policy;

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

    @Mock
    private Environment environment;

    @Test
    void testConstructorAppliesFallbacksWhenPropertiesMissing() {
        // 1.Arrange / 2.Exercise
        final PresentationLimitsPolicy policy = new PresentationLimitsPolicy(environment);

        // 3.Assert
        Assertions.assertEquals(3, policy.getCounterpartyRecentReviewsLimit());
        Assertions.assertEquals(4, policy.getCarDetailSimilarListingsLimit());
    }

    @Test
    void testConstructorAppliesFallbacksWhenPropertiesNonPositive() {
        // 1.Arrange
        Mockito.when(environment.getProperty(COUNTERPARTY_KEY, Integer.class)).thenReturn(0);
        Mockito.when(environment.getProperty(SIMILAR_KEY, Integer.class)).thenReturn(-1);

        // 2.Exercise
        final PresentationLimitsPolicy policy = new PresentationLimitsPolicy(environment);

        // 3.Assert
        Assertions.assertEquals(3, policy.getCounterpartyRecentReviewsLimit());
        Assertions.assertEquals(4, policy.getCarDetailSimilarListingsLimit());
    }

    @Test
    void testConstructorUsesPropertyValuesWhenPositive() {
        // 1.Arrange
        Mockito.when(environment.getProperty(COUNTERPARTY_KEY, Integer.class)).thenReturn(5);
        Mockito.when(environment.getProperty(SIMILAR_KEY, Integer.class)).thenReturn(8);

        // 2.Exercise
        final PresentationLimitsPolicy policy = new PresentationLimitsPolicy(environment);

        // 3.Assert
        Assertions.assertEquals(5, policy.getCounterpartyRecentReviewsLimit());
        Assertions.assertEquals(8, policy.getCarDetailSimilarListingsLimit());
    }
}
