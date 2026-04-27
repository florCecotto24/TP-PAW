package ar.edu.itba.paw.webapp.security;

import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.services.UserService;

public class RydenUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public RydenUserDetailsService(final UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(final String email) throws UsernameNotFoundException {
        final User user = userService.findByEmailForAuthentication(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user for email"));
        if (!Boolean.TRUE.equals(user.getEmailValidated().orElse(false))) {
            throw new UsernameNotFoundException("Email not validated");
        }
        final String hash = user.getPasswordHash()
                .filter(h -> !h.isBlank())
                .orElseThrow(() -> new UsernameNotFoundException("User has no password"));
        final List<String> roleNames = userService.findRoleNamesForUser(user.getId());
        final List<GrantedAuthority> authorities = UserRoleAuthorities.fromDbRoleNames(roleNames);
        return new RydenUserDetails(
                user.getId(),
                user.getEmail(),
                user.getForename(),
                user.getSurname(),
                hash,
                authorities);
    }
}
