package ar.edu.itba.paw.webapp.security;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.UserService;

public class RydenUserDetailsService implements UserDetailsService {

    static final String ROLE_USER_AUTHORITY = "ROLE_USER";

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
        return new RydenUserDetails(
                user.getId(),
                user.getEmail(),
                user.getForename(),
                user.getSurname(),
                hash,
                Collections.singletonList(new SimpleGrantedAuthority(ROLE_USER_AUTHORITY)));
    }
}
