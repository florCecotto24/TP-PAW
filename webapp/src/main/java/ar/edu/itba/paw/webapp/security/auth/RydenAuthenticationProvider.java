package ar.edu.itba.paw.webapp.security.auth;

import java.util.List;
import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.security.auth.exception.EmailNotValidatedException;
import ar.edu.itba.paw.webapp.security.auth.exception.LegacyPasswordMailedException;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.security.auth.userdetails.UserRoleAuthorities;

/**
 * Email and password authentication backed by {@link UserService}, BCrypt, and email-validation rules.
 */
@Component
public final class RydenAuthenticationProvider implements AuthenticationProvider {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public RydenAuthenticationProvider(
            final UserService userService,
            final PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        final String email = authentication.getName();
        final String rawPassword = authentication.getCredentials() != null ? authentication.getCredentials().toString() : "";

        final User user = userService.findByEmailForAuthentication(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (user.isBlocked()) {
            throw new DisabledException("Account is blocked");
        }

        final String hash = user.getPasswordHash().filter(h -> !h.isBlank()).orElse(null);
        if (hash == null) {
            final Locale locale = LocaleContextHolder.getLocale();
            userService.assignRandomPasswordAndEmailForLegacyUser(user.getId(), user.getEmail(), locale);
            throw new LegacyPasswordMailedException();
        }

        if (!passwordEncoder.matches(rawPassword, hash)) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!Boolean.TRUE.equals(user.getEmailValidated().orElse(false))) {
            throw new EmailNotValidatedException(user.getEmail());
        }

        final List<GrantedAuthority> authorities =
                UserRoleAuthorities.fromDbRoleNames(userService.findRoleNamesForUser(user.getId()));
        final RydenUserDetails principal = new RydenUserDetails(
                user.getId(),
                user.getEmail(),
                user.getForename(),
                user.getSurname(),
                hash,
                authorities,
                user.getRoleAssignedBy().orElse(null));

        return new UsernamePasswordAuthenticationToken(principal, rawPassword, principal.getAuthorities());
    }

    @Override
    public boolean supports(final Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
