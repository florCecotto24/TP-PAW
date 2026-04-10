package ar.edu.itba.paw.webapp.security;

import java.util.Collections;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.EmailVerificationService;
import ar.edu.itba.paw.services.UserService;

@Component
public class RydenAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RydenAuthenticationProvider.class);

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public RydenAuthenticationProvider(
            final UserService userService,
            final EmailVerificationService emailVerificationService,
            final PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.emailVerificationService = emailVerificationService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        final String email = authentication.getName();
        final String rawPassword = authentication.getCredentials() != null ? authentication.getCredentials().toString() : "";

        final User user = userService.findByEmailForAuthentication(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!Boolean.TRUE.equals(user.getEmailValidated().orElse(false))) {
            final Locale locale = LocaleContextHolder.getLocale();
            try {
                emailVerificationService.issueNewCode(user.getId(), user.getEmail(), locale);
            } catch (final Exception e) {
                LOG.error("Could not issue email verification code for userId={}", user.getId(), e);
            }
            throw new EmailNotValidatedException(user.getEmail());
        }

        final String hash = user.getPasswordHash().filter(h -> !h.isBlank()).orElse(null);
        if (hash == null) {
            throw new RegistrationIncompleteException(user.getId());
        }

        if (!passwordEncoder.matches(rawPassword, hash)) {
            throw new BadCredentialsException("Invalid credentials");
        }

        final RydenUserDetails principal = new RydenUserDetails(
                user.getId(),
                user.getEmail(),
                user.getForename(),
                user.getSurname(),
                hash,
                Collections.singletonList(new SimpleGrantedAuthority(RydenUserDetailsService.ROLE_USER_AUTHORITY)));

        return new UsernamePasswordAuthenticationToken(principal, rawPassword, principal.getAuthorities());
    }

    @Override
    public boolean supports(final Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
