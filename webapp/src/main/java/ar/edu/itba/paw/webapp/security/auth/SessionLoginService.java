package ar.edu.itba.paw.webapp.security.auth;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.security.auth.userdetails.UserRoleAuthorities;

/**
 * Establishes an authenticated session after post-registration password set, email verification, or password reset.
 */
@Component
public final class SessionLoginService {

    private final UserService userService;
    private final SecurityContextRepository securityContextRepository;

    public SessionLoginService(
            final UserService userService,
            final SecurityContextRepository securityContextRepository) {
        this.userService = userService;
        this.securityContextRepository = securityContextRepository;
    }

    public void signInUserAfterEmailVerification(final HttpServletRequest request, final HttpServletResponse response, final long userId) {
        final User basic = userService.getUserById(userId).orElseThrow();
        final User withHash = userService.findByEmailForAuthentication(basic.getEmail())
                .orElseThrow(() -> new IllegalStateException("User missing after verification"));
        establishSessionForUser(request, response, withHash);
    }

    /** After forgot-password flow: user has just set a new password for this email. */
    public void signInUserAfterPasswordReset(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final String email) {
        final User withHash = userService.findByEmailForAuthentication(email.trim())
                .orElseThrow(() -> new IllegalStateException("User missing after password reset"));
        establishSessionForUser(request, response, withHash);
    }

    private void establishSessionForUser(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final User withHash) {
        final String hash = withHash.getPasswordHash().filter(h -> !h.isBlank())
                .orElseThrow(() -> new IllegalStateException("User has no password hash for session"));
        final List<GrantedAuthority> authorities =
                UserRoleAuthorities.fromDbRoleNames(userService.findRoleNamesForUser(withHash.getId()));
        final RydenUserDetails principal = new RydenUserDetails(
                withHash.getId(),
                withHash.getEmail(),
                withHash.getForename(),
                withHash.getSurname(),
                hash,
                authorities,
                withHash.getRoleAssignedBy().orElse(null));
        final UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        final SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        this.securityContextRepository.saveContext(context, request, response);
    }
}
