package ar.edu.itba.paw.webapp.security.auth.userdetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
     * Builds authorities from typed {@link UserRole} values. {@code null} entries are skipped.
     * If nothing maps, grants {@link UserRole#USER} so authenticated accounts keep a minimal role
     * set (legacy safety).
     */
    public static List<GrantedAuthority> fromUserRoles(final Collection<UserRole> roles) {
        final List<GrantedAuthority> list = new ArrayList<>();
        for (final UserRole role : roles) {
            if (role != null) {
                list.add(new SimpleGrantedAuthority(role.springAuthorityName()));
            }
        }
        if (list.isEmpty()) {
            list.add(new SimpleGrantedAuthority(UserRole.USER.springAuthorityName()));
        }
        return list;
    }
}
