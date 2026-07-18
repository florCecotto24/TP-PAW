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
import org.springframework.core.env.Environment;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.CarValidationException;
import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;

@ExtendWith(MockitoExtension.class)
class CarAvailabilityPolicyTest {

    private static final String NEW_KEY = "app.listing.max-availability-forward-wall-days";
    private static final String LEGACY_KEY = "app.listing.max-availability-total-days";

    @Mock
    private Environment environment;

    @Test
    void testConstructorPrefersNewKeyOverLegacy() {
        // 1.Arrange: only NEW_KEY is consulted in this branch — the SUT short-circuits before
        // looking at LEGACY_KEY when NEW_KEY resolves to a positive value.
        Mockito.when(environment.getProperty(NEW_KEY, Integer.class)).thenReturn(120);

        // 2.Act
        final CarAvailabilityPolicy policy = new CarAvailabilityPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(120, policy.getMaxAvailabilityForwardWallDays());
    }

    @Test
    void testConstructorFallsBackToLegacyKeyWhenNewKeyMissing() {
        // 1.Arrange: leave NEW_KEY unstubbed (resolves to null) so the SUT falls back to LEGACY_KEY.
        // lenient() avoids strict-stubs flagging the LEGACY_KEY stub as a potential problem on the
        // NEW_KEY call that returns null.
        Mockito.lenient().when(environment.getProperty(LEGACY_KEY, Integer.class)).thenReturn(60);

        // 2.Act
        final CarAvailabilityPolicy policy = new CarAvailabilityPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(60, policy.getMaxAvailabilityForwardWallDays());
    }

    @Test
    void testConstructorUsesDefaultWhenBothKeysMissingOrInvalid() {
        // 1.Arrange
        Mockito.when(environment.getProperty(NEW_KEY, Integer.class)).thenReturn(0);
        Mockito.when(environment.getProperty(LEGACY_KEY, Integer.class)).thenReturn(-1);

        // 2.Act
        final CarAvailabilityPolicy policy = new CarAvailabilityPolicyImpl(environment);

        // 3.Assert
        Assertions.assertEquals(365, policy.getMaxAvailabilityForwardWallDays());
    }

    @Test
    void testValidateAvailabilityWithinPublishHorizonAllowsEmptyPeriods() {
        // 1.Arrange
        final CarAvailabilityPolicy policy = new CarAvailabilityPolicyImpl(environment);
        final LocalDate today = LocalDate.of(2026, 5, 3);

        // 2.Act / 3.Assert (no exception)
        Assertions.assertDoesNotThrow(() -> policy.validateAvailabilityWithinPublishHorizon(today, List.of()));
        Assertions.assertDoesNotThrow(() -> policy.validateAvailabilityWithinPublishHorizon(today, null));
    }

    @Test
    void testValidateAvailabilityWithinPublishHorizonRejectsNullReferenceDay() {
        // 1.Arrange
        final CarAvailabilityPolicy policy = new CarAvailabilityPolicyImpl(environment);

        // 2.Act / 3.Assert
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

        // 2.Act / 3.Assert
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

        // 2.Act
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

        // 2.Act / 3.Assert
        Assertions.assertThrows(CarValidationException.class,
                () -> policy.validateAvailabilityWithinPublishHorizon(today, periods));
    }

    @Test
    void testValidateAvailabilityWithinPublishHorizonThrowsWhenEndIsBeforeReferenceDay() {
        // 1.Arrange — A10: period end must not be before today (reference wall day)
        final CarAvailabilityPolicy policy = new CarAvailabilityPolicyImpl(environment);
        final LocalDate today = LocalDate.of(2026, 5, 3);
        final List<AvailabilityPeriod> periods = List.of(
                new AvailabilityPeriod(today.minusDays(10), today.minusDays(1)));

        // 2.Act
        final CarValidationException ex = Assertions.assertThrows(CarValidationException.class,
                () -> policy.validateAvailabilityWithinPublishHorizon(today, periods));

        // 3.Assert
        Assertions.assertEquals(MessageKeys.CAR_AVAILABILITY_INCLUDES_PAST_DATES, ex.getMessageCode());
    }

    @Test
    void testValidateAvailabilityWithinPublishHorizonAcceptsEndOnReferenceDay() {
        // 1.Arrange
        final CarAvailabilityPolicy policy = new CarAvailabilityPolicyImpl(environment);
        final LocalDate today = LocalDate.of(2026, 5, 3);
        final List<AvailabilityPeriod> periods = List.of(
                new AvailabilityPeriod(today.minusDays(5), today));

        // 2.Act / 3.Assert
        Assertions.assertDoesNotThrow(() -> policy.validateAvailabilityWithinPublishHorizon(today, periods));
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

        // 2.Act / 3.Assert
        Assertions.assertDoesNotThrow(() -> policy.validateAvailabilityWithinPublishHorizon(today, periods));
    }
}
