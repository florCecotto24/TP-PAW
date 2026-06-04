package ar.edu.itba.paw.webapp.security.auth;

import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import ar.edu.itba.paw.models.security.UserRole;

/**
 * Stateless helpers to inspect Spring Security {@link Authentication} objects.
 *
 * Centralises the {@link UserRole#ADMIN} authority check that several components (HTTP {@code access()}
 * managers, the STOMP channel interceptor, controllers that need to vary the page model for admins)
 * used to inline as an authorities loop.
 */
public final class AuthenticationAuthorities {

    private static final String ROLE_ADMIN_AUTHORITY = UserRole.ADMIN.springAuthorityName();

    private AuthenticationAuthorities() {
    }

    /**
     * {@code true} when {@code authentication} represents an authenticated, non-anonymous principal
     * carrying the {@link UserRole#ADMIN} granted authority.
     */
    public static boolean hasAdminRole(@Nullable final Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        for (final GrantedAuthority authority : authentication.getAuthorities()) {
            if (ROLE_ADMIN_AUTHORITY.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
