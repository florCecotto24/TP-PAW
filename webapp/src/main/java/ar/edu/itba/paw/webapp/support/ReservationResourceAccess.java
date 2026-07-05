package ar.edu.itba.paw.webapp.support;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.reservation.ReservationParticipantRole;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.webapp.security.auth.AuthenticationAuthorities;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * Authorization predicates for reservation resources.
 *
 * <p>See {@link UserResourceAccess} for the {@code is*} vs. {@code require*} split rationale. The
 * {@code require*} methods remain only where the check is conditional on parsed PATCH body content
 * (the reservation controller's PATCH endpoint routes {@code carReturned}/{@code startDate} to
 * different roles depending on which fields the caller sent) or where the outcome is a
 * business-routing value rather than a pure gate ({@link #requireParticipantRole}).
 */
@Component
public final class ReservationResourceAccess {

    private static final String ACCESS_DENIED_MESSAGE = "You do not have permission to perform this action.";

    private final ReservationService reservationService;

    public ReservationResourceAccess(final ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public boolean isAdmin() {
        return AuthenticationAuthorities.hasAdminRole(SecurityContextHolder.getContext().getAuthentication());
    }

    public boolean canViewReservation(final long reservationId, final RydenUserDetails viewer) {
        if (isAdmin()) {
            return true;
        }
        if (viewer == null) {
            return false;
        }
        final long userId = viewer.getUserId();
        return reservationService.getRiderReservationById(userId, reservationId).isPresent()
                || reservationService.getOwnerReservationById(userId, reservationId).isPresent();
    }

    public boolean isRider(final long reservationId, final RydenUserDetails viewer) {
        return viewer != null
                && reservationService.getRiderReservationById(viewer.getUserId(), reservationId).isPresent();
    }

    public boolean isOwner(final long reservationId, final RydenUserDetails viewer) {
        return viewer != null
                && reservationService.getOwnerReservationById(viewer.getUserId(), reservationId).isPresent();
    }

    public boolean isSelfOrAdmin(final long userId, final RydenUserDetails viewer) {
        return viewer != null && (viewer.getUserId() == userId || isAdmin());
    }

    public void requireViewReservation(final long reservationId, final RydenUserDetails viewer) {
        if (!canViewReservation(reservationId, viewer)) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
    }

    public void requireSelfOrAdmin(final long userId, final RydenUserDetails viewer) {
        if (!isSelfOrAdmin(userId, viewer)) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
    }

    /** The hat the signed-in participant wears for this reservation; denies otherwise. */
    public ReservationParticipantRole requireParticipantRole(final long reservationId, final RydenUserDetails viewer) {
        if (viewer == null || isAdmin()) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
        if (isRider(reservationId, viewer)) {
            return ReservationParticipantRole.RIDER;
        }
        if (isOwner(reservationId, viewer)) {
            return ReservationParticipantRole.OWNER;
        }
        throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
    }

    public void requireRider(final long reservationId, final RydenUserDetails viewer) {
        if (!isRider(reservationId, viewer)) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
    }

    public void requireOwner(final long reservationId, final RydenUserDetails viewer) {
        if (!isOwner(reservationId, viewer)) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
    }
}
