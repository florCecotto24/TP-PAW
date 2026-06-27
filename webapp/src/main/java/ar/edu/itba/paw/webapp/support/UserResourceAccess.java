package ar.edu.itba.paw.webapp.support;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.security.auth.AuthenticationAuthorities;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/** Authorization helpers for user profile resources. */
@Component
public final class UserResourceAccess {

    public boolean canViewPrivate(final long userId, final RydenUserDetails viewer) {
        if (viewer == null) {
            return false;
        }
        if (viewer.getUserId() == userId) {
            return true;
        }
        return AuthenticationAuthorities.hasAdminRole(
                SecurityContextHolder.getContext().getAuthentication());
    }

    public void requireSelf(final long userId, final RydenUserDetails viewer) {
        if (viewer == null || viewer.getUserId() != userId) {
            throw new javax.ws.rs.ForbiddenException();
        }
    }

    public void requireSelfOrAdmin(final long userId, final RydenUserDetails viewer) {
        if (!canViewPrivate(userId, viewer)) {
            throw new javax.ws.rs.ForbiddenException();
        }
    }

    public void requirePrivateView(final long userId, final RydenUserDetails viewer) {
        requireSelfOrAdmin(userId, viewer);
    }

    public void requireAdmin() {
        if (!AuthenticationAuthorities.hasAdminRole(
                SecurityContextHolder.getContext().getAuthentication())) {
            throw new javax.ws.rs.ForbiddenException();
        }
    }
}
