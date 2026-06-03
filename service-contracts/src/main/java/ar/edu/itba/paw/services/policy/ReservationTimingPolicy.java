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

    /**
     * Grace window (days) after which the review auto-skip scheduler closes a stale review with a
     * null/commentless row. Rider window starts at the reservation {@code endDate}; owner window starts
     * at the moment the owner marked the car returned. A value less than 1 disables the auto-skip job.
     */
    int getReviewAutoSkipDays();
}
