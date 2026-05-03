package ar.edu.itba.paw.services.policy;

import java.time.LocalTime;

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
class ListingCheckInOutPolicyTest {

    private static final String KEY = "app.listing.min-hours-between-checkin-checkout";

    @Mock
    private Environment environment;

    @Test
    void testConstructorAppliesDefaultWhenPropertyMissing() {
        // 1.Arrange / 2.Exercise
        final ListingCheckInOutPolicy policy = new ListingCheckInOutPolicy(environment);

        // 3.Assert
        Assertions.assertEquals(6, policy.getMinHoursBetweenCheckInAndCheckOut());
    }

    @Test
    void testConstructorAppliesDefaultWhenPropertyIsZeroOrNegative() {
        // 1.Arrange
        Mockito.when(environment.getProperty(KEY, Integer.class)).thenReturn(0);

        // 2.Exercise
        final ListingCheckInOutPolicy policy = new ListingCheckInOutPolicy(environment);

        // 3.Assert
        Assertions.assertEquals(6, policy.getMinHoursBetweenCheckInAndCheckOut());
    }

    @Test
    void testConstructorUsesPositivePropertyValue() {
        // 1.Arrange
        Mockito.when(environment.getProperty(KEY, Integer.class)).thenReturn(8);

        // 2.Exercise
        final ListingCheckInOutPolicy policy = new ListingCheckInOutPolicy(environment);

        // 3.Assert
        Assertions.assertEquals(8, policy.getMinHoursBetweenCheckInAndCheckOut());
    }

    @Test
    void testHasMinimumGapReturnsTrueWhenEitherTimeIsNull() {
        // 1.Arrange
        final ListingCheckInOutPolicy policy = new ListingCheckInOutPolicy(environment);

        // 2.Exercise / 3.Assert
        Assertions.assertTrue(policy.hasMinimumGap(null, LocalTime.of(18, 0)));
        Assertions.assertTrue(policy.hasMinimumGap(LocalTime.of(8, 0), null));
        Assertions.assertTrue(policy.hasMinimumGap(null, null));
    }

    @Test
    void testHasMinimumGapReturnsTrueWhenCheckOutNotAfterCheckInToDelegateToOtherValidators() {
        // 1.Arrange
        final ListingCheckInOutPolicy policy = new ListingCheckInOutPolicy(environment);

        // 2.Exercise / 3.Assert
        Assertions.assertTrue(policy.hasMinimumGap(LocalTime.of(10, 0), LocalTime.of(10, 0)));
        Assertions.assertTrue(policy.hasMinimumGap(LocalTime.of(18, 0), LocalTime.of(8, 0)));
    }

    @Test
    void testHasMinimumGapReturnsTrueAtExactGap() {
        // 1.Arrange: default gap = 6h.
        final ListingCheckInOutPolicy policy = new ListingCheckInOutPolicy(environment);

        // 2.Exercise / 3.Assert
        Assertions.assertTrue(policy.hasMinimumGap(LocalTime.of(8, 0), LocalTime.of(14, 0)));
    }

    @Test
    void testHasMinimumGapReturnsFalseWhenGapIsTooSmall() {
        // 1.Arrange: default gap = 6h; difference here is 5h59m.
        final ListingCheckInOutPolicy policy = new ListingCheckInOutPolicy(environment);

        // 2.Exercise / 3.Assert
        Assertions.assertFalse(policy.hasMinimumGap(LocalTime.of(8, 0), LocalTime.of(13, 59)));
    }

    @Test
    void testHasMinimumGapHonoursConfiguredHoursFromEnvironment() {
        // 1.Arrange: override min hours to 4.
        Mockito.when(environment.getProperty(KEY, Integer.class)).thenReturn(4);
        final ListingCheckInOutPolicy policy = new ListingCheckInOutPolicy(environment);

        // 2.Exercise / 3.Assert: 4h exact passes; 3h59m fails.
        Assertions.assertTrue(policy.hasMinimumGap(LocalTime.of(8, 0), LocalTime.of(12, 0)));
        Assertions.assertFalse(policy.hasMinimumGap(LocalTime.of(8, 0), LocalTime.of(11, 59)));
    }
}
