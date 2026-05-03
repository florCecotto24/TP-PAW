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
class ReservationTimingPolicyTest {

    @Mock
    private Environment environment;

    @Test
    void testConstructorAppliesDefaultsWhenAllPropertiesMissing() {
        // 1.Arrange

        // 2.Exercise
        final ReservationTimingPolicy policy = new ReservationTimingPolicy(environment);

        // 3.Assert
        Assertions.assertEquals(24, policy.getPickupLeadHours());
        Assertions.assertEquals(12, policy.getPaymentProofDeadlineHours());
        Assertions.assertEquals(2, policy.getPaymentProofReminderLeadHours());
        Assertions.assertEquals(2, policy.getReturnReminderHoursBeforeCheckout());
        Assertions.assertEquals(30, policy.getMaxBillableDaysPerReservation());
    }

    @Test
    void testConstructorUsesPropertyValuesWhenAboveOne() {
        // 1.Arrange
        Mockito.when(environment.getProperty("app.reservation.pickup-lead-hours", Integer.class)).thenReturn(48);
        Mockito.when(environment.getProperty("app.reservation.payment-proof-deadline-hours", Integer.class))
                .thenReturn(6);
        Mockito.when(environment.getProperty("app.reservation.payment-proof-reminder-lead-hours", Integer.class))
                .thenReturn(1);
        Mockito.when(environment.getProperty("app.reservation.return-reminder-hours-before-checkout", Integer.class))
                .thenReturn(4);
        Mockito.when(environment.getProperty("app.reservation.max-billable-days", Integer.class)).thenReturn(60);

        // 2.Exercise
        final ReservationTimingPolicy policy = new ReservationTimingPolicy(environment);

        // 3.Assert
        Assertions.assertEquals(48, policy.getPickupLeadHours());
        Assertions.assertEquals(6, policy.getPaymentProofDeadlineHours());
        Assertions.assertEquals(1, policy.getPaymentProofReminderLeadHours());
        Assertions.assertEquals(4, policy.getReturnReminderHoursBeforeCheckout());
        Assertions.assertEquals(60, policy.getMaxBillableDaysPerReservation());
    }

    @Test
    void testConstructorFallsBackToDefaultsForZeroOrNegativeProperties() {
        // 1.Arrange
        Mockito.when(environment.getProperty("app.reservation.pickup-lead-hours", Integer.class)).thenReturn(0);
        Mockito.when(environment.getProperty("app.reservation.payment-proof-deadline-hours", Integer.class))
                .thenReturn(-2);
        Mockito.when(environment.getProperty("app.reservation.payment-proof-reminder-lead-hours", Integer.class))
                .thenReturn(0);
        Mockito.when(environment.getProperty("app.reservation.return-reminder-hours-before-checkout", Integer.class))
                .thenReturn(-1);
        Mockito.when(environment.getProperty("app.reservation.max-billable-days", Integer.class)).thenReturn(0);

        // 2.Exercise
        final ReservationTimingPolicy policy = new ReservationTimingPolicy(environment);

        // 3.Assert
        Assertions.assertEquals(24, policy.getPickupLeadHours());
        Assertions.assertEquals(12, policy.getPaymentProofDeadlineHours());
        Assertions.assertEquals(2, policy.getPaymentProofReminderLeadHours());
        Assertions.assertEquals(2, policy.getReturnReminderHoursBeforeCheckout());
        Assertions.assertEquals(30, policy.getMaxBillableDaysPerReservation());
    }
}
