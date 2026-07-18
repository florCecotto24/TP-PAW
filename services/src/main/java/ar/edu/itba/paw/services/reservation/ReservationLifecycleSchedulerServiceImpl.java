package ar.edu.itba.paw.services.reservation;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.email.reservation.RiderCarReturnEmailPayload;
import ar.edu.itba.paw.models.email.reservation.RiderReviewInviteEmailPayload;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.policy.ReservationTimingPolicy;
import ar.edu.itba.paw.util.ReservationMailComposer;

/**
 * Scheduled lifecycle jobs (return reminders, return checkout notifications, rider review
 * invitations, automatic review-skipping) plus per-car analytics consumed by the owner car
 * detail dashboard. Kept apart from the rider-facing workflow so each file stays focused and
 * within the project's size budget.
 *
 * Architectural rule: this service no longer touches {@code ReservationDao} — row reads
 * and mutations are funneled through {@link ReservationService} (the sole DAO owner).
 */
@Service
public class ReservationLifecycleSchedulerServiceImpl implements ReservationLifecycleSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationLifecycleSchedulerServiceImpl.class);

    private final ReservationService reservationService;
    private final ReservationTimingPolicy reservationTimingPolicy;
    private final ReservationMailComposer mailComposer;
    private final ReservationLifecycleRowProcessor lifecycleRowProcessor;

    @Autowired
    public ReservationLifecycleSchedulerServiceImpl(
            @Lazy final ReservationService reservationService,
            final ReservationTimingPolicy reservationTimingPolicy,
            final ReservationMailComposer mailComposer,
            final ReservationLifecycleRowProcessor lifecycleRowProcessor) {
        this.reservationService = reservationService;
        this.reservationTimingPolicy = reservationTimingPolicy;
        this.mailComposer = mailComposer;
        this.lifecycleRowProcessor = lifecycleRowProcessor;
    }

    // ---------------------------------------------------------------------------------------
    // Return reminder + checkout + review invite emails
    // ---------------------------------------------------------------------------------------

    /**
     * Deliberately NOT {@code @Transactional}: each claim commits in {@code REQUIRES_NEW} before
     * {@code @Async} mail is queued, so a rolled-back batch cannot leave emails for unclaimed rows.
     */
    @Override
    public void dispatchReturnReminderEmails() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final int hours = reservationTimingPolicy.getReturnReminderHoursBeforeCheckout();
        final List<Reservation> candidates = reservationService.findReservationsForReturnReminderEmail(now, hours);
        LOGGER.atInfo().addArgument(candidates.size()).addArgument(hours)
                .log("Return reminder run: {} candidate reservation(s) (within {} h before checkout)");
        int queued = 0;
        for (final Reservation reservation : candidates) {
            final Optional<RiderCarReturnEmailPayload> payload = mailComposer.buildRiderCarReturnPayload(reservation);
            if (payload.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId())
                        .log("Skipping return reminder: missing data (reservation id={})");
                continue;
            }
            if (!lifecycleRowProcessor.claimReturnReminder(reservation.getId())) {
                continue;
            }
            try {
                mailComposer.sendRiderReturnReminder(payload.get());
                queued++;
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId())
                        .log("Failed to queue return reminder email (reservation id={})");
            }
        }
        LOGGER.atInfo().addArgument(queued).log("Return reminder run: queued {} email(s)");
    }

    /**
     * Deliberately NOT {@code @Transactional}: each claim commits in {@code REQUIRES_NEW} before
     * {@code @Async} mail is queued, so a rolled-back batch cannot leave emails for unclaimed rows.
     */
    @Override
    public void dispatchReturnCheckoutEmails() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final List<Reservation> candidates = reservationService.findReservationsForReturnCheckoutEmail(now);
        LOGGER.atInfo().addArgument(candidates.size()).log("Return checkout email run: {} candidate reservation(s)");
        int queued = 0;
        for (final Reservation reservation : candidates) {
            final Optional<RiderCarReturnEmailPayload> payload = mailComposer.buildRiderCarReturnPayload(reservation);
            if (payload.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId())
                        .log("Skipping return checkout mail: missing data (reservation id={})");
                continue;
            }
            if (!lifecycleRowProcessor.claimReturnCheckout(reservation.getId())) {
                continue;
            }
            try {
                mailComposer.sendRiderReturnCheckout(payload.get());
                queued++;
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId())
                        .log("Failed to queue return checkout email (reservation id={})");
            }
        }
        LOGGER.atInfo().addArgument(queued).log("Return checkout email run: queued {} email(s)");
    }

    /**
     * Deliberately NOT {@code @Transactional}: each claim commits in {@code REQUIRES_NEW} before
     * {@code @Async} mail is queued.
     */
    @Override
    public void dispatchRiderReviewInviteEmails() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final List<Reservation> candidates = reservationService.findReservationsForRiderReviewInviteEmail(now);
        LOGGER.atInfo().addArgument(candidates.size()).log("Rider review invite run: {} candidate reservation(s)");
        int queued = 0;
        for (final Reservation reservation : candidates) {
            final Optional<RiderReviewInviteEmailPayload> payload = mailComposer.buildRiderReviewInvitePayload(reservation);
            if (payload.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId())
                        .log("Skipping rider review invite: missing data (reservation id={})");
                continue;
            }
            if (!lifecycleRowProcessor.claimRiderReviewInvite(reservation.getId())) {
                continue;
            }
            try {
                mailComposer.sendRiderReviewInvite(payload.get());
                queued++;
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId())
                        .log("Failed to queue rider review invite email (reservation id={})");
            }
        }
        LOGGER.atInfo().addArgument(queued).log("Rider review invite run: queued {} email(s)");
    }

    /**
     * Deliberately NOT {@code @Transactional}: each auto-skip commits in {@code REQUIRES_NEW}.
     * Candidates are already filtered with {@code NOT EXISTS} in the DAO (no per-row review probe).
     */
    @Override
    public void dispatchReviewAutoSkips() {
        final int days = reservationTimingPolicy.getReviewAutoSkipDays();
        if (days < 1) {
            LOGGER.atInfo().addArgument(days).log("Review auto-skip run skipped: feature disabled (days={})");
            return;
        }
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final OffsetDateTime endDateCutoff = now.minusDays(days);
        final OffsetDateTime carReturnedAtCutoff = now.minusDays(days);

        // Rider side: window starts at the rental endDate.
        final List<Reservation> riderCandidates =
                reservationService.findReservationsForRiderReviewAutoSkip(now, endDateCutoff);
        LOGGER.atInfo().addArgument(riderCandidates.size()).addArgument(days)
                .log("Review auto-skip (rider) run: {} candidate reservation(s) (window {} days)");
        int rDone = 0;
        for (final Reservation r : riderCandidates) {
            try {
                lifecycleRowProcessor.autoSkipRiderReview(r.getRiderId(), r.getId());
                rDone++;
            } catch (final RuntimeException e) {
                LOGGER.atWarn().setCause(e).addArgument(r.getId())
                        .log("Auto-skip rider review failed (reservation id={})");
            }
        }
        LOGGER.atInfo().addArgument(rDone).log("Review auto-skip (rider) run: closed {} review(s)");

        // Owner side: window starts at the moment the owner marked the car returned.
        final List<Reservation> ownerCandidates =
                reservationService.findReservationsForOwnerReviewAutoSkip(now, carReturnedAtCutoff);
        LOGGER.atInfo().addArgument(ownerCandidates.size()).addArgument(days)
                .log("Review auto-skip (owner) run: {} candidate reservation(s) (window {} days)");
        int oDone = 0;
        for (final Reservation r : ownerCandidates) {
            final long ownerId = Optional.ofNullable(r.getCar())
                    .map(Car::getOwner)
                    .map(User::getId)
                    .orElse(0L);
            if (ownerId == 0L) {
                continue;
            }
            try {
                lifecycleRowProcessor.autoSkipOwnerReview(ownerId, r.getId());
                oDone++;
            } catch (final RuntimeException e) {
                LOGGER.atWarn().setCause(e).addArgument(r.getId())
                        .log("Auto-skip owner review failed (reservation id={})");
            }
        }
        LOGGER.atInfo().addArgument(oDone).log("Review auto-skip (owner) run: closed {} review(s)");
    }

    /**
     * Deliberately NOT {@code @Transactional}: orchestrates per-row work via
     * {@link ReservationLifecycleRowProcessor} ({@code REQUIRES_NEW} each). An outer TX would
     * hold locks across the whole batch.
     */
    @Override
    public void transitionAcceptedReservationsToStarted() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final List<Long> candidateIds = reservationService.findAcceptedReservationIdsWithStartOnOrBefore(now);
        LOGGER.atInfo().addArgument(candidateIds.size())
                .log("Reservation start transition run: {} accepted reservation(s) at or past pickup time");
        int started = 0;
        for (final Long reservationId : candidateIds) {
            try {
                started += lifecycleRowProcessor.transitionAcceptedToStartedIfDue(reservationId, now);
            } catch (final RuntimeException e) {
                LOGGER.atWarn().setCause(e).addArgument(reservationId)
                        .log("Accepted→started transition failed (reservation id={})");
            }
        }
        LOGGER.atInfo().addArgument(started).log("Reservation start transition run: marked {} reservation(s) as started");
    }

    // ---------------------------------------------------------------------------------------
    // Per-car analytics
    // ---------------------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countCarReservationsByStatus(final long ownerId, final long carId) {
        return mergeCancelledBuckets(reservationService.countCarReservationsByStatus(ownerId, carId));
    }

    /** Keeps a single {@code cancelled} counter for owner dashboards while persisting granular statuses. */
    private static Map<String, Long> mergeCancelledBuckets(final Map<String, Long> raw) {
        final Map<String, Long> out = new LinkedHashMap<>(raw);
        final String cancelled = Reservation.Status.CANCELLED.dbValue();
        long merged = out.getOrDefault(cancelled, 0L);
        for (final Reservation.Status granular : List.of(
                Reservation.Status.CANCELLED_BY_RIDER,
                Reservation.Status.CANCELLED_BY_OWNER,
                Reservation.Status.CANCELLED_DUE_TO_MISSING_PAYMENT_PROOF)) {
            merged += out.getOrDefault(granular.dbValue(), 0L);
            out.remove(granular.dbValue());
        }
        out.put(cancelled, merged);
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCarTotalEarnings(final long ownerId, final long carId) {
        return reservationService.sumCarRevenueByStatuses(
                ownerId, carId, List.of(
                        Reservation.Status.ACCEPTED.dbValue(),
                        Reservation.Status.STARTED.dbValue(),
                        Reservation.Status.FINISHED.dbValue()));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCarPendingEarnings(final long ownerId, final long carId) {
        return reservationService.sumCarRevenueByStatuses(
                ownerId, carId, List.of(
                        Reservation.Status.ACCEPTED.dbValue(),
                        Reservation.Status.STARTED.dbValue()));
    }

    @Override
    @Transactional(readOnly = true)
    public long getCarTotalDaysRented(final long ownerId, final long carId) {
        return reservationService.sumCarFinishedBillableDays(ownerId, carId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCarReservationsThisMonth(final long ownerId, final long carId) {
        final YearMonth current = YearMonth.now(AppTimezone.WALL_ZONE);
        final OffsetDateTime monthStart =
                current.atDay(1).atStartOfDay(AppTimezone.WALL_ZONE).toOffsetDateTime();
        final OffsetDateTime nextMonthStart =
                current.plusMonths(1).atDay(1).atStartOfDay(AppTimezone.WALL_ZONE).toOffsetDateTime();
        return reservationService.countCarReservationsCreatedBetween(ownerId, carId, monthStart, nextMonthStart);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OffsetDateTime> getCarNextReservationDate(final long ownerId, final long carId) {
        return reservationService.findCarNextActiveReservationDate(
                ownerId, carId, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
