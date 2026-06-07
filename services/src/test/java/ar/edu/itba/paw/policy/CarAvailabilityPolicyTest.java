package ar.edu.itba.paw.policy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.CarValidationException;
import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CarAvailabilityPolicyTest {

    private static final String NEW_KEY = "app.listing.max-availability-forward-wall-days";
    private static final String LEGACY_KEY = "app.listing.max-availability-total-days";

    @Mock
    private Environment environment;

    @Test
    void testConstructorPrefersNewKeyOverLegacy() {
        // 1.Arrange
        Mockito.when(environment.getProperty(NEW_KEY, Integer.class)).thenReturn(120);
        Mockito.when(environment.getProperty(LEGACY_KEY, Integer.class)).thenReturn(60);

        // 2.Exercise
        final CarAvailabilityPolicy policy = new CarAvailabilityPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(120, policy.getMaxAvailabilityForwardWallDays());
    }

    @Test
    void testConstructorFallsBackToLegacyKeyWhenNewKeyMissing() {
        // 1.Arrange
        Mockito.when(environment.getProperty(LEGACY_KEY, Integer.class)).thenReturn(60);

        // 2.Exercise
        final CarAvailabilityPolicy policy = new CarAvailabilityPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(60, policy.getMaxAvailabilityForwardWallDays());
    }

    @Test
    void testConstructorUsesDefaultWhenBothKeysMissingOrInvalid() {
        // 1.Arrange
        Mockito.when(environment.getProperty(NEW_KEY, Integer.class)).thenReturn(0);
        Mockito.when(environment.getProperty(LEGACY_KEY, Integer.class)).thenReturn(-1);

        // 2.Exercise
        final CarAvailabilityPolicy policy = new CarAvailabilityPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(365, policy.getMaxAvailabilityForwardWallDays());
    }

    @Test
    void testValidateAvailabilityWithinPublishHorizonAllowsEmptyPeriods() {
        // 1.Arrange
        final CarAvailabilityPolicy policy = new CarAvailabilityPolicyImpl(environment);
        final LocalDate today = LocalDate.of(2026, 5, 3);

        // 2.Exercise / 3.Assert (no exception)
        Assertions.assertDoesNotThrow(() -> policy.validateAvailabilityWithinPublishHorizon(today, List.of()));
        Assertions.assertDoesNotThrow(() -> policy.validateAvailabilityWithinPublishHorizon(today, null));
    }

    @Test
    void testValidateAvailabilityWithinPublishHorizonRejectsNullReferenceDay() {
        // 1.Arrange
        final CarAvailabilityPolicy policy = new CarAvailabilityPolicyImpl(environment);

        // 2.Exercise / 3.Assert
        Assertions.assertThrows(NullPointerException.class,
                () -> policy.validateAvailabilityWithinPublishHorizon(null, List.of()));
    }

    @Test
    void testValidateAvailabilityWithinPublishHorizonAcceptsPeriodsWithinWindow() {
        // 1.Arrange: horizon = 365 (default), today + 100 days well inside.
        final CarAvailabilityPolicy policy = new CarAvailabilityPolicyImpl(environment);
        final LocalDate today = LocalDate.of(2026, 5, 3);
        final List<AvailabilityPeriod> periods = List.of(
                new AvailabilityPeriod(today.plusDays(10), today.plusDays(50)),
                new AvailabilityPeriod(today, today.plusDays(365)));

        // 2.Exercise / 3.Assert
        Assertions.assertDoesNotThrow(() -> policy.validateAvailabilityWithinPublishHorizon(today, periods));
    }

    @Test
    void testValidateAvailabilityWithinPublishHorizonThrowsWhenStartIsBeyondHorizon() {
        // 1.Arrange
        Mockito.when(environment.getProperty(NEW_KEY, Integer.class)).thenReturn(30);
        final CarAvailabilityPolicy policy = new CarAvailabilityPolicyImpl(environment);
        final LocalDate today = LocalDate.of(2026, 5, 3);
        final List<AvailabilityPeriod> periods = List.of(
                new AvailabilityPeriod(today.plusDays(31), today.plusDays(40)));

        // 2.Exercise
        final CarValidationException ex = Assertions.assertThrows(CarValidationException.class,
                () -> policy.validateAvailabilityWithinPublishHorizon(today, periods));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.CAR_AVAILABILITY_BEYOND_PUBLISH_HORIZON, ex.getMessageCode());
        Assertions.assertArrayEquals(new Object[]{30}, ex.getMessageArgs());
    }

    @Test
    void testValidateAvailabilityWithinPublishHorizonThrowsWhenEndIsBeyondHorizon() {
        // 1.Arrange
        Mockito.when(environment.getProperty(NEW_KEY, Integer.class)).thenReturn(30);
        final CarAvailabilityPolicy policy = new CarAvailabilityPolicyImpl(environment);
        final LocalDate today = LocalDate.of(2026, 5, 3);
        final List<AvailabilityPeriod> periods = List.of(
                new AvailabilityPeriod(today.plusDays(20), today.plusDays(40)));

        // 2.Exercise / 3.Assert
        Assertions.assertThrows(CarValidationException.class,
                () -> policy.validateAvailabilityWithinPublishHorizon(today, periods));
    }

    @Test
    void testValidateAvailabilityWithinPublishHorizonSkipsInvalidOrderedPeriods() {
        // 1.Arrange: end-before-start period would normally be beyond horizon, but the policy must skip it.
        Mockito.when(environment.getProperty(NEW_KEY, Integer.class)).thenReturn(10);
        final CarAvailabilityPolicy policy = new CarAvailabilityPolicyImpl(environment);
        final LocalDate today = LocalDate.of(2026, 5, 3);
        final List<AvailabilityPeriod> periods = new ArrayList<>();
        periods.add(new AvailabilityPeriod(today.plusDays(99), today.plusDays(50)));
        periods.add(null);

        // 2.Exercise / 3.Assert
        Assertions.assertDoesNotThrow(() -> policy.validateAvailabilityWithinPublishHorizon(today, periods));
    }
}
