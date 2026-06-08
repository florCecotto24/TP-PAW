package ar.edu.itba.paw.webapp.security.auth.userdetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import ar.edu.itba.paw.models.security.UserRole;

/**
 * Maps {@link UserRole} values to Spring {@link GrantedAuthority} instances. Single place to adjust
 * when {@link UserRole} grows or authority naming changes.
 */
public final class UserRoleAuthorities {

    private UserRoleAuthorities() {
    }

    /**
     * Builds the authority set for a user from typed {@link UserRole} values. {@code null} entries
     * are skipped. If nothing maps, grants {@link UserRole#USER} so authenticated accounts keep a
     * minimal role set (legacy safety).
     *
     * Returns a {@link Set} because Spring authorities have set semantics (no duplicates).
     * Uses {@link LinkedHashSet} to preserve insertion order, which keeps log output stable when
     * a user has multiple roles.
     */
    public static Set<GrantedAuthority> fromUserRoles(final Collection<UserRole> roles) {
        final Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (final UserRole role : roles) {
            if (role != null) {
                authorities.add(new SimpleGrantedAuthority(role.springAuthorityName()));
            }
        }
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority(UserRole.USER.springAuthorityName()));
        }
        return authorities;
    }
}
