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

import ar.edu.itba.paw.models.pagination.PaginationFallbackSizes;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaginationPolicyTest {

    @Mock
    private Environment environment;

    @Test
    void testConstructorAppliesAllFallbacksWhenEnvironmentIsEmpty() {
        // 1.Arrange / 2.Exercise
        final PaginationPolicy policy = new PaginationPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(PaginationFallbackSizes.UI_PAGE_SIZE, policy.getUiPageSize());
        Assertions.assertEquals(PaginationFallbackSizes.DB_FETCH_SIZE, policy.getDbFetchSize());
        Assertions.assertEquals(PaginationFallbackSizes.UI_PAGE_SIZE, policy.getDefaultPageSize(),
                "default-page-size legacy key absent → mirrors uiPageSize");
        Assertions.assertEquals(PaginationFallbackSizes.LISTING_PUBLIC_REVIEWS_PAGE_SIZE,
                policy.getListingPublicReviewsPageSize());
    }

    @Test
    void testConstructorUsesPropertyValuesWhenAllPositive() {
        // 1.Arrange
        Mockito.when(environment.getProperty("app.pagination.ui-page-size", Integer.class)).thenReturn(10);
        Mockito.when(environment.getProperty("app.pagination.db-fetch-size", Integer.class)).thenReturn(40);
        Mockito.when(environment.getProperty("app.pagination.default-page-size", Integer.class)).thenReturn(20);
        Mockito.when(environment.getProperty("app.pagination.listing-public-reviews-page-size", Integer.class))
                .thenReturn(7);

        // 2.Exercise
        final PaginationPolicy policy = new PaginationPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(10, policy.getUiPageSize());
        Assertions.assertEquals(40, policy.getDbFetchSize());
        Assertions.assertEquals(20, policy.getDefaultPageSize());
        Assertions.assertEquals(7, policy.getListingPublicReviewsPageSize());
    }

    @Test
    void testConstructorClampsDbFetchSizeToBeAtLeastUiPageSize() {
        // 1.Arrange: db < ui, must be clamped up.
        Mockito.when(environment.getProperty("app.pagination.ui-page-size", Integer.class)).thenReturn(20);
        Mockito.when(environment.getProperty("app.pagination.db-fetch-size", Integer.class)).thenReturn(5);

        // 2.Exercise
        final PaginationPolicy policy = new PaginationPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(20, policy.getUiPageSize());
        Assertions.assertEquals(20, policy.getDbFetchSize(),
                "db-fetch-size must be raised to ui-page-size when smaller");
    }

    @Test
    void testConstructorIgnoresNonPositivePropertyValuesAndUsesFallbacks() {
        // 1.Arrange
        Mockito.when(environment.getProperty("app.pagination.ui-page-size", Integer.class)).thenReturn(0);
        Mockito.when(environment.getProperty("app.pagination.db-fetch-size", Integer.class)).thenReturn(-3);
        Mockito.when(environment.getProperty("app.pagination.default-page-size", Integer.class)).thenReturn(0);
        Mockito.when(environment.getProperty("app.pagination.listing-public-reviews-page-size", Integer.class))
                .thenReturn(-1);

        // 2.Exercise
        final PaginationPolicy policy = new PaginationPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(PaginationFallbackSizes.UI_PAGE_SIZE, policy.getUiPageSize());
        Assertions.assertEquals(PaginationFallbackSizes.DB_FETCH_SIZE, policy.getDbFetchSize());
        Assertions.assertEquals(PaginationFallbackSizes.UI_PAGE_SIZE, policy.getDefaultPageSize());
        Assertions.assertEquals(PaginationFallbackSizes.LISTING_PUBLIC_REVIEWS_PAGE_SIZE,
                policy.getListingPublicReviewsPageSize());
    }
}
