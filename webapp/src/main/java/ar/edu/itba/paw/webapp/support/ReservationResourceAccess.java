package ar.edu.itba.paw.webapp.support;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.webapp.security.auth.AuthenticationAuthorities;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/** Authorization helpers for reservation resources. */
@Component
public final class ReservationResourceAccess {

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

    public void requireViewReservation(final long reservationId, final RydenUserDetails viewer) {
        if (!canViewReservation(reservationId, viewer)) {
            throw new javax.ws.rs.ForbiddenException();
        }
    }

    public void requireSelfOrAdmin(final long userId, final RydenUserDetails viewer) {
        if (viewer == null) {
            throw new javax.ws.rs.ForbiddenException();
        }
        if (viewer.getUserId() == userId) {
            return;
        }
        if (isAdmin()) {
            return;
        }
        throw new javax.ws.rs.ForbiddenException();
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new javax.ws.rs.ForbiddenException();
        }
    }

    /** {@code "rider"} or {@code "owner"} for the signed-in participant; 403 otherwise. */
    public String requireParticipantRole(final long reservationId, final RydenUserDetails viewer) {
        if (viewer == null) {
            throw new javax.ws.rs.ForbiddenException();
        }
        if (isAdmin()) {
            throw new javax.ws.rs.ForbiddenException();
        }
        final long userId = viewer.getUserId();
        if (reservationService.getRiderReservationById(userId, reservationId).isPresent()) {
            return "rider";
        }
        if (reservationService.getOwnerReservationById(userId, reservationId).isPresent()) {
            return "owner";
        }
        throw new javax.ws.rs.ForbiddenException();
    }

    public void requireRider(final long reservationId, final RydenUserDetails viewer) {
        if (viewer == null
                || reservationService.getRiderReservationById(viewer.getUserId(), reservationId).isEmpty()) {
            throw new javax.ws.rs.ForbiddenException();
        }
    }

    public void requireOwner(final long reservationId, final RydenUserDetails viewer) {
        if (viewer == null
                || reservationService.getOwnerReservationById(viewer.getUserId(), reservationId).isEmpty()) {
            throw new javax.ws.rs.ForbiddenException();
        }
    }
}
