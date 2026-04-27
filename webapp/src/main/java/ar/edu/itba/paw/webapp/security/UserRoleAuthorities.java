package ar.edu.itba.paw.webapp.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import ar.edu.itba.paw.models.security.UserRole;

/**
 * Maps persisted role names to Spring {@link GrantedAuthority} instances. Single place to adjust when
 * {@link UserRole} grows or authority naming changes.
 */
public final class UserRoleAuthorities {

    private UserRoleAuthorities() {
    }

    /**
     * Builds authorities from {@code user_roles} strings. Unknown names are skipped. If nothing maps,
     * grants {@link UserRole#USER} so authenticated accounts keep a minimal role set (legacy safety).
     */
    public static List<GrantedAuthority> fromDbRoleNames(final Collection<String> roleNamesFromDb) {
        final List<GrantedAuthority> list = new ArrayList<>();
        for (final String raw : roleNamesFromDb) {
            UserRole.fromPersistenceName(raw)
                    .map(r -> new SimpleGrantedAuthority(r.springAuthorityName()))
                    .ifPresent(list::add);
        }
        if (list.isEmpty()) {
            list.add(new SimpleGrantedAuthority(UserRole.USER.springAuthorityName()));
        }
        return list;
    }
}
