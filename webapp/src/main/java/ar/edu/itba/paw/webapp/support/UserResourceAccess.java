package ar.edu.itba.paw.webapp.support;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.security.auth.AuthenticationAuthorities;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * Authorization predicates for user profile resources.
 *
 * <p>The {@code is*} boolean methods are the primary API: they back {@code @PreAuthorize} on the
 * JAX-RS resources, so Spring Security's method-security interceptor enforces them declaratively
 * before the resource method runs, throwing {@link AccessDeniedException} on denial (mapped to a
 * 401/403 {@code vnd.paw.error} body by {@code AccessDeniedExceptionMapper}). The {@code require*}
 * methods remain only for the checks that are conditional on parsed request-body content — e.g.
 * {@code UserController#patchUser} routes password/profile/admin fields to different checks
 * depending on which fields the caller actually sent, which cannot be a single method-level
 * precondition.
 */
@Component
public final class UserResourceAccess {

    private static final String ACCESS_DENIED_MESSAGE = "You do not have permission to perform this action.";

    public boolean isAdmin() {
        return AuthenticationAuthorities.hasAdminRole(SecurityContextHolder.getContext().getAuthentication());
    }

    public boolean isSelf(final long userId, final RydenUserDetails viewer) {
        return viewer != null && viewer.getUserId() == userId;
    }

    public boolean isSelfOrAdmin(final long userId, final RydenUserDetails viewer) {
        return isSelf(userId, viewer) || isAdmin();
    }

    public void requireSelf(final long userId, final RydenUserDetails viewer) {
        if (!isSelf(userId, viewer)) {
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
}
