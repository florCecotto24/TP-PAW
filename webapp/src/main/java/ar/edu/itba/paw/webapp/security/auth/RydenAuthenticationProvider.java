package ar.edu.itba.paw.webapp.security.auth;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.exception.user.OtpAttemptsExceededException;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.services.user.EmailVerificationService;
import ar.edu.itba.paw.services.user.PasswordResetService;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.security.auth.exception.LegacyPasswordMailedException;
import ar.edu.itba.paw.webapp.security.auth.exception.OtpRateLimitedAuthenticationException;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.security.auth.userdetails.UserRoleAuthorities;

/**
 * Email + secret authentication for the REST API:
 *
 * <ul>
 *   <li>Verified account: {@code Basic email:password} (BCrypt).</li>
 *   <li>Email confirmation (OTP): {@code Basic email:otp} — consumes the verification code and marks the email verified.</li>
 *   <li>Password reset (OTP): {@code Basic email:otp} — grants {@link RydenAuthorities#PASSWORD_RESET_OTP}; the code is consumed on {@code PATCH /users/{id}} with the new password.</li>
 * </ul>
 */
@Component
public final class RydenAuthenticationProvider implements AuthenticationProvider {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final PasswordEncoder passwordEncoder;

    public RydenAuthenticationProvider(
            final UserService userService,
            final EmailVerificationService emailVerificationService,
            final PasswordResetService passwordResetService,
            final PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.emailVerificationService = emailVerificationService;
        this.passwordResetService = passwordResetService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        final String email = authentication.getName();
        final String rawSecret = authentication.getCredentials() != null
                ? authentication.getCredentials().toString()
                : "";

        User user = userService.findByEmailForAuthentication(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        final boolean emailVerified = Boolean.TRUE.equals(user.getEmailValidated().orElse(false));

        if (!emailVerified) {
            return authenticateWithEmailVerificationOtp(email, rawSecret);
        }

        final String hash = user.getPasswordHash().filter(h -> !h.isBlank()).orElse(null);
        if (hash == null) {
            final Locale locale = LocaleContextHolder.getLocale();
            userService.assignRandomPasswordAndEmailForLegacyUser(user.getId(), user.getEmail(), locale);
            throw new LegacyPasswordMailedException();
        }

        if (passwordEncoder.matches(rawSecret, hash)) {
            return buildAuthentication(user, rawSecret, UserRoleAuthorities.fromUserRoles(
                    userService.findRolesForUser(user.getId())));
        }

        try {
            if (passwordResetService.matchesResetCode(email, rawSecret)) {
                final Set<GrantedAuthority> authorities = new java.util.LinkedHashSet<>(
                        UserRoleAuthorities.fromUserRoles(userService.findRolesForUser(user.getId())));
                authorities.add(new SimpleGrantedAuthority(RydenAuthorities.PASSWORD_RESET_OTP));
                return buildAuthentication(user, rawSecret, authorities);
            }
        } catch (final OtpAttemptsExceededException ex) {
            throw new OtpRateLimitedAuthenticationException(ex);
        }

        throw new BadCredentialsException("Invalid credentials");
    }

    private Authentication authenticateWithEmailVerificationOtp(final String email, final String otp) {
        try {
            emailVerificationService.verifyEmailAndConsumeCode(email, otp);
        } catch (final OtpAttemptsExceededException ex) {
            throw new OtpRateLimitedAuthenticationException(ex);
        } catch (final RydenException ex) {
            throw new BadCredentialsException("Invalid credentials");
        }
        final User verified = userService.findByEmailForAuthentication(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        return buildAuthentication(verified, otp, UserRoleAuthorities.fromUserRoles(
                userService.findRolesForUser(verified.getId())));
    }

    private Authentication buildAuthentication(
            final User user,
            final String rawSecret,
            final Set<GrantedAuthority> authorities) {
        final String hash = user.getPasswordHash().orElse("");
        final RydenUserDetails principal = new RydenUserDetails(
                user.getId(),
                user.getEmail(),
                user.getForename(),
                user.getSurname(),
                hash,
                new ArrayList<>(authorities),
                user.getRoleAssignedBy().orElse(null));
        return new UsernamePasswordAuthenticationToken(principal, rawSecret, principal.getAuthorities());
    }

    @Override
    public boolean supports(final Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
