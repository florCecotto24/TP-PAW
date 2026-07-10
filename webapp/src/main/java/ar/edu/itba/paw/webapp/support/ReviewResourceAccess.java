package ar.edu.itba.paw.webapp.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.review.Review;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

import javax.ws.rs.ForbiddenException;

/**
 * Visibility rules for canonical review resources ({@code GET /reviews}, {@code GET /reviews/{id}}).
 *
 * API rule: a single-resource GET that the caller may not see yields {@code 403}. A collection
 * GET never fails for per-item visibility — invisible items are omitted from the payload.
 */
@Component
public final class ReviewResourceAccess {

    private final ReservationResourceAccess reservationResourceAccess;

    @Autowired
    public ReviewResourceAccess(final ReservationResourceAccess reservationResourceAccess) {
        this.reservationResourceAccess = reservationResourceAccess;
    }

    /**
     * Rated reviews are public (car page, user profile). Omitted reviews (no rating) are visible
     * only to reservation participants and admins.
     */
    public boolean canViewReview(final Review review, final RydenUserDetails viewer) {
        if (review.getRating().isPresent()) {
            return true;
        }
        return reservationResourceAccess.canViewReservation(review.getReservationId(), viewer);
    }

    /** Single-resource gate: deny with {@code 403} when the caller cannot see the review. */
    public void requireCanViewReview(final Review review, final RydenUserDetails viewer) {
        if (!canViewReview(review, viewer)) {
            throw new ForbiddenException();
        }
    }
}
