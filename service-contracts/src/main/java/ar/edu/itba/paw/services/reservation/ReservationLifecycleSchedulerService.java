package ar.edu.itba.paw.services.reservation;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Scheduled jobs and per-car analytics that depend on reservation data but are not part of
 * the rider-facing lifecycle. Specifically:
 *
 * <ul>
 *   <li>Return reminder / return checkout / rider review invite emails.</li>
 *   <li>Auto-skipping stale rider and owner reviews.</li>
 *   <li>The car detail dashboard counters: status counts, earnings, days rented,
 *       reservations this month, and the next active reservation date.</li>
 * </ul>
 *
 * <p>Payment / refund reminders and the refund-overdue sweep live in
 * {@link ReservationPaymentService}.
 */
public interface ReservationLifecycleSchedulerService {

    /** Scheduled job: reminder email to the rider the day before pickup. */
    int dispatchReservationReminderEmails();

    /** Scheduled job: reminder email to return the car. Returns the number of emails queued. */
    int dispatchReturnReminderEmails();

    /** Scheduled job: email at checkout if the car was not marked returned. Returns the number of emails queued. */
    int dispatchReturnCheckoutEmails();

    /** Scheduled job: invite the rider to leave an optional review after the rental period. Returns the number of emails queued. */
    int dispatchRiderReviewInviteEmails();

    /**
     * Scheduled job: closes stale reviews by inserting a null/commentless "skipped" review row.
     * Window length is {@code app.reservation.review-auto-skip-days} (less than 1 disables the job).
     */
    int dispatchReviewAutoSkips();

    /**
     * Scheduled job: marks confirmed ({@code accepted}) reservations as {@code started} once pickup time is reached.
     */
    int transitionAcceptedReservationsToStarted();

    /** Counts reservations per status bucket for the owner's car dashboard charts. */
    Map<String, Long> countCarReservationsByStatus(long ownerId, long carId);

    /** Sum of {@code total_price} for {@code accepted}/{@code started}/{@code finished} reservations on that car. */
    BigDecimal getCarTotalEarnings(long ownerId, long carId);

    /** Sum of {@code total_price} for {@code accepted}/{@code started} reservations on that car. */
    BigDecimal getCarPendingEarnings(long ownerId, long carId);

    /** Sum of billable days across {@code finished} reservations for that car. */
    long getCarTotalDaysRented(long ownerId, long carId);

    /** Reservations for that car whose {@code created_at} falls in the current UTC calendar month. */
    long getCarReservationsThisMonth(long ownerId, long carId);

    /** Earliest {@code start_date} of an {@code accepted} or {@code started} reservation strictly after now. */
    Optional<OffsetDateTime> getCarNextReservationDate(long ownerId, long carId);
}
