package ar.edu.itba.paw.services.reservation;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.services.review.ReviewService;

/**
 * Per-row {@code REQUIRES_NEW} claim helpers for lifecycle / chat-digest email jobs. Extracted so
 * Spring's proxy honours the new-transaction boundary (self-invocation would not). Callers send
 * {@code @Async} mail only after a successful claim commit.
 */
@Component
public class ReservationLifecycleRowProcessor {

    private final ReservationService reservationService;
    private final ReservationMessageService reservationMessageService;
    private final ReviewService reviewService;

    @Autowired
    public ReservationLifecycleRowProcessor(
            @Lazy final ReservationService reservationService,
            @Lazy final ReservationMessageService reservationMessageService,
            @Lazy final ReviewService reviewService) {
        this.reservationService = reservationService;
        this.reservationMessageService = reservationMessageService;
        this.reviewService = reviewService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimReturnReminder(final long reservationId) {
        return reservationService.claimReturnReminderEmailSent(reservationId) > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimPickupReminder(final long reservationId) {
        return reservationService.claimPickupReminderEmailSent(reservationId) > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimReturnCheckout(final long reservationId) {
        return reservationService.claimReturnCheckoutEmailSent(reservationId) > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimRiderReviewInvite(final long reservationId) {
        return reservationService.claimRiderReviewInviteEmailSent(reservationId) > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markChatDigestNotified(final Collection<Long> messageIds) {
        return reservationMessageService.markEmailNotified(messageIds) > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int transitionAcceptedToStartedIfDue(final long reservationId, final java.time.OffsetDateTime now) {
        return reservationService.transitionAcceptedToStartedIfDue(reservationId, now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void autoSkipRiderReview(final long riderId, final long reservationId) {
        reviewService.submitRiderReviewOfOwner(riderId, reservationId, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void autoSkipOwnerReview(final long ownerId, final long reservationId) {
        reviewService.submitOwnerReviewOfRider(ownerId, reservationId, null, null);
    }
}
