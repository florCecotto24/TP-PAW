package ar.edu.itba.paw.services.reservation;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.services.user.UserService;

/**
 * Per-row {@code REQUIRES_NEW} helpers for the reminder / owner-blocking sweeps. Extracted into their
 * own {@code @Component} so the new-transaction boundary is honoured by the Spring proxy (a self-call
 * would bypass it). Each state mutation (claiming a reminder flag, blocking an owner) commits on its own
 * BEFORE the caller sends the corresponding {@code @Async} email, so a rolled-back batch can never leave
 * an email dispatched for a change that did not persist (which would re-send on the next run), and one
 * failing row cannot roll back the rest of the batch.
 */
@Component
public class ReservationSweepRowProcessor {

    private final ReservationService reservationService;
    private final UserService userService;

    @Autowired
    public ReservationSweepRowProcessor(
            @Lazy final ReservationService reservationService,
            final UserService userService) {
        this.reservationService = reservationService;
        this.userService = userService;
    }

    /** Atomically claims the due-payment-proof reminder for this reservation. True iff this call won it. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimDuePaymentProofReminder(final long reservationId) {
        return reservationService.claimPendingPaymentProofEmailSent(reservationId) > 0;
    }

    /** Atomically claims the due-refund-proof reminder for this reservation. True iff this call won it. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimDueRefundProofReminder(final long reservationId) {
        return reservationService.claimPendingRefundEmailSent(reservationId) > 0;
    }

    /**
     * Blocks the owner in its own transaction iff they exist and are not already blocked.
     *
     * @return the owner if this call actually blocked them (so the caller can email them), else empty.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<User> blockOwnerForRefundOverdueIfEligible(final long ownerId) {
        final Optional<User> ownerOpt = userService.getUserById(ownerId);
        if (ownerOpt.isEmpty() || ownerOpt.get().isBlocked()) {
            return Optional.empty();
        }
        userService.blockUser(ownerId);
        return ownerOpt;
    }
}
