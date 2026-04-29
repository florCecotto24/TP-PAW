package ar.edu.itba.paw.services.policy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Business time windows for reservations, read from {@code application/application.properties}.
 */
@Component
public final class ReservationTimingPolicy {

    private final int pickupLeadHours;
    private final int paymentProofDeadlineHours;
    private final int returnReminderHoursBeforeCheckout;
    private final int maxBillableDaysPerReservation;

    @Autowired
    public ReservationTimingPolicy(final Environment environment) {
        this.pickupLeadHours = readPositiveInt(environment, "app.reservation.pickup-lead-hours", 24);
        this.paymentProofDeadlineHours =
                readPositiveInt(environment, "app.reservation.payment-proof-deadline-hours", 12);
        this.returnReminderHoursBeforeCheckout =
                readPositiveInt(environment, "app.reservation.return-reminder-hours-before-checkout", 2);
        this.maxBillableDaysPerReservation = readPositiveInt(environment, "app.reservation.max-billable-days", 30);
    }

    private static int readPositiveInt(final Environment env, final String key, final int defaultValue) {
        final Integer v = env.getProperty(key, Integer.class);
        if (v == null || v < 1) {
            return defaultValue;
        }
        return v;
    }

    public int getPickupLeadHours() {
        return pickupLeadHours;
    }

    public int getPaymentProofDeadlineHours() {
        return paymentProofDeadlineHours;
    }

    public int getReturnReminderHoursBeforeCheckout() {
        return returnReminderHoursBeforeCheckout;
    }

    /** Inclusive wall-calendar billable days allowed for one reservation ({@code app.reservation.max-billable-days}). */
    public int getMaxBillableDaysPerReservation() {
        return maxBillableDaysPerReservation;
    }
}
