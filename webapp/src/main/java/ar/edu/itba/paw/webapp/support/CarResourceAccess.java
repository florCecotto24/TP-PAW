package ar.edu.itba.paw.webapp.support;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.webapp.security.auth.AuthenticationAuthorities;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/** Authorization helpers for car resources. */
@Component
public final class CarResourceAccess {

    public boolean isAdmin() {
        return AuthenticationAuthorities.hasAdminRole(SecurityContextHolder.getContext().getAuthentication());
    }

    public boolean isOwnerOrAdmin(final Car car, final RydenUserDetails viewer) {
        if (viewer == null) {
            return false;
        }
        if (car.getOwnerId() == viewer.getUserId()) {
            return true;
        }
        return isAdmin();
    }

    public void requireOwnerOrAdmin(final Car car, final RydenUserDetails viewer) {
        if (!isOwnerOrAdmin(car, viewer)) {
            throw new javax.ws.rs.ForbiddenException();
        }
    }

    public void requireOwner(final Car car, final RydenUserDetails viewer) {
        if (viewer == null || car.getOwnerId() != viewer.getUserId()) {
            throw new javax.ws.rs.ForbiddenException();
        }
    }

    public void requireSelfOrAdminForOwnerQuery(final long ownerId, final RydenUserDetails viewer) {
        if (viewer == null) {
            return;
        }
        if (viewer.getUserId() == ownerId) {
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
}
