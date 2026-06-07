package ar.edu.itba.paw.services.reservation;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.car.CarAvailability;

/**
 * Pricing, date math, configurable timing policy getters, and rider-side validation guards
 * for the reservation flow. Separated from {@link ReservationWorkflowService} so the submit
 * and edit endpoints can re-use the exact same validations without duplicating the
 * underlying calendar / billable-day math.
 *
 * <p>Validation methods throw {@link ar.edu.itba.paw.exception.reservation.RiderReservationException}
 * with the appropriate {@link ar.edu.itba.paw.exception.MessageKeys} when the rider input
 * violates the rule.
 */
public interface ReservationPricingService {

    /**
     * Per-day pricing plan for a reservation: the accumulated total, the distinct set of
     * winning availability ids in their first appearance order, and the availability that
     * covers the pickup day. The workflow service uses these to persist the reservation and
     * its covering availability bridge in one shot.
     */
    record ReservationPlan(
            BigDecimal total,
            LinkedHashSet<Long> coveringAvailabilityIds,
            CarAvailability firstDayAvailability) {
    }

    // ---------------------------------------------------------------------------------------
    // Timing policy
    // ---------------------------------------------------------------------------------------

    /** Minimum hours between "now" and the pickup. */
    int getConfiguredPickupLeadHours();

    /** Hours to upload payment proof after creating a pending reservation. */
    int getConfiguredPaymentProofDeadlineHours();

    /** Hours before {@code end_date} to email the rider to return the vehicle. */
    int getConfiguredReturnReminderHoursBeforeCheckout();

    /** Max inclusive billable days for one reservation. */
    int getConfiguredMaxReservationBillableDays();

    // ---------------------------------------------------------------------------------------
    // Pricing / day math
    // ---------------------------------------------------------------------------------------

    /**
     * Normalizes a client-supplied total string (digits and optional single decimal point);
     * empty when invalid.
     */
    Optional<String> normalizeClientReservationTotal(String reservationTotal);

    /**
     * Formatted total for UI when {@code carId} and wall-local range parse; uses car
     * availability day price.
     */
    Optional<String> reservationTotalDisplayByCar(Long carId, String fromDateTime, String untilDateTime);

    /** Total price for the car and UTC interval using configured billable-day rules. */
    Optional<BigDecimal> calculateTotalByCar(long carId, OffsetDateTime startDate, OffsetDateTime endDate);

    /** Inclusive billable rental days from pickup/return instants in the wall zone. */
    long calculateBillableDays(OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Day-by-day pricing plan for a reservation: for each wall-calendar day in
     * {@code [firstBillableDay, lastBillableDay]}, picks the {@code OFFERED} availability of
     * the car that wins by latest {@code createdAt}. Returns empty when any day lacks an
     * effective offered availability.
     */
    Optional<ReservationPlan> planReservationByCar(long carId, LocalDate firstBillableDay, LocalDate lastBillableDay);

    // ---------------------------------------------------------------------------------------
    // Rider input validations (throw on violation)
    // ---------------------------------------------------------------------------------------

    /** Rejects pickup days strictly before today in the wall zone. */
    void validateWallPickupDateNotBeforeToday(OffsetDateTime startDate);

    /** Rejects pickups that do not honour the configured lead-time policy. */
    void validatePickupAtLeastConfiguredLeadAhead(OffsetDateTime startDate);

    /**
     * Defensive check against tampered submissions: the wall-time component of
     * {@code startDate} must equal the {@code checkInTime} of the effective availability for
     * the pickup day, and the wall-time component of {@code endDate} must equal the
     * {@code checkOutTime} of the effective availability for the return day.
     */
    void validateHandoverTimesMatchEffectiveAvailability(long carId, OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Whether every day in the interval is covered by an {@code OFFERED} availability of
     * {@code carId}. When {@code availabilityId} is non-null, the entire interval must fit
     * inside that single availability row.
     */
    boolean reservationIntervalFitsCarAvailability(
            long carId, Long availabilityId, OffsetDateTime startDate, OffsetDateTime endDate);
}
