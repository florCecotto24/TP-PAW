package ar.edu.itba.paw.services.reservation;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.reservation.Reservation;

/**
 * Per-row transactional helper for the payment-proof expiration sweep. Extracted into its own
 * {@code @Component} (rather than a private method on {@link ReservationPaymentServiceImpl}) so the
 * {@code REQUIRES_NEW} boundary is honoured: a self-invocation through {@code this.} would bypass the
 * Spring proxy and never start a new transaction. Each cancellation therefore commits (or rolls back)
 * on its own, so one failing row cannot roll back the rest of the batch, the per-row pessimistic lock
 * is released promptly instead of being held for the whole sweep, and the cancellation email (sent by
 * the caller only when this returns a non-empty result) is dispatched strictly AFTER the row committed.
 */
@Component
public class ExpiredPaymentProofRowCanceller {

    private final ReservationService reservationService;

    @Autowired
    public ExpiredPaymentProofRowCanceller(@Lazy final ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * Cancels the reservation in its own transaction iff it is still an expired, receipt-less PENDING row
     * (the guard lives in {@code cancelPendingMissingPaymentProofIfEligible}, under a pessimistic write
     * lock, so a concurrent payment-proof upload cannot be clobbered — see the TOCTOU guard).
     *
     * @return the refreshed reservation if it was actually cancelled, or empty if it no longer qualified
     *         (e.g. the rider paid in the meantime) or could not be reloaded.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Reservation> cancelExpiredReservation(final long reservationId, final OffsetDateTime now) {
        if (reservationService.cancelPendingMissingPaymentProofIfEligible(reservationId, now) <= 0) {
            return Optional.empty();
        }
        // JOIN FETCH car/owner/catalog before this REQUIRES_NEW TX ends — the caller sends mail
        // after commit, outside any persistence session.
        return reservationService.getReservationById(reservationId);
    }
}
