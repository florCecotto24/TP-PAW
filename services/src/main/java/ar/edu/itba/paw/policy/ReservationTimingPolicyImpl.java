package ar.edu.itba.paw.policy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Reads {@code app.reservation.*} properties to back {@link ReservationTimingPolicy}. */
@Component
public final class ReservationTimingPolicyImpl implements ReservationTimingPolicy {

    private final int pickupLeadHours;
    private final int paymentProofDeadlineHours;
    private final int paymentProofReminderLeadHours;
    private final int returnReminderHoursBeforeCheckout;
    private final int maxBillableDaysPerReservation;
    private final int reviewAutoSkipDays;

    @Autowired
    public ReservationTimingPolicyImpl(final Environment environment) {
        this.pickupLeadHours = readPositiveInt(environment, "app.reservation.pickup-lead-hours", 24);
        this.paymentProofDeadlineHours =
                readPositiveInt(environment, "app.reservation.payment-proof-deadline-hours", 12);
        this.paymentProofReminderLeadHours =
                readPositiveInt(environment, "app.reservation.payment-proof-reminder-lead-hours", 2);
        this.returnReminderHoursBeforeCheckout =
                readPositiveInt(environment, "app.reservation.return-reminder-hours-before-checkout", 2);
        this.maxBillableDaysPerReservation = readPositiveInt(environment, "app.reservation.max-billable-days", 30);
        this.reviewAutoSkipDays = readNonNegativeInt(environment, "app.reservation.review-auto-skip-days", 15);
    }

    private static int readPositiveInt(final Environment env, final String key, final int defaultValue) {
        final Integer v = env.getProperty(key, Integer.class);
        if (v == null || v < 1) {
            return defaultValue;
        }
        return v;
    }

    private static int readNonNegativeInt(final Environment env, final String key, final int defaultValue) {
        final Integer v = env.getProperty(key, Integer.class);
        if (v == null || v < 0) {
            return defaultValue;
        }
        return v;
    }

    @Override
    public int getPickupLeadHours() {
        return pickupLeadHours;
    }

    @Override
    public int getPaymentProofDeadlineHours() {
        return paymentProofDeadlineHours;
    }

    @Override
    public int getPaymentProofReminderLeadHours() {
        return paymentProofReminderLeadHours;
    }

    @Override
    public int getReturnReminderHoursBeforeCheckout() {
        return returnReminderHoursBeforeCheckout;
    }

    @Override
    public int getMaxBillableDaysPerReservation() {
        return maxBillableDaysPerReservation;
    }

    @Override
    public int getReviewAutoSkipDays() {
        return reviewAutoSkipDays;
    }
}
