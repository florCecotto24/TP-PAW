package ar.edu.itba.paw.services.policy;

/** Business time windows for reservations. */
public interface ReservationTimingPolicy {

    int getPickupLeadHours();

    int getPaymentProofDeadlineHours();

    /** Lead window for the pending payment-proof rider email. */
    int getPaymentProofReminderLeadHours();

    int getReturnReminderHoursBeforeCheckout();

    /** Inclusive wall-calendar billable days allowed for one reservation. */
    int getMaxBillableDaysPerReservation();
}
