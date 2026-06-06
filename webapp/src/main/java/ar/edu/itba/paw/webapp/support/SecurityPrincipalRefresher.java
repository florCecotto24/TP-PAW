package ar.edu.itba.paw.webapp.support;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * Encapsulates the {@link SecurityContextHolder} / {@link SecurityContextRepository} dance needed
 * to refresh the {@link RydenUserDetails} principal in the current request without forcing the user
 * to log in again. Used after profile mutations that change the display name so subsequent JSPs
 * (header, etc.) read the new value from the authenticated principal rather than a stale snapshot.
 *
 * <p>Lives in {@code webapp} because the principal type and the request/response coupling are
 * pure web-tier concerns; no service should know about either.</p>
 */
@Component
public final class SecurityPrincipalRefresher {

    private final SecurityContextRepository securityContextRepository;

    public SecurityPrincipalRefresher(final SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * Rebuilds the current {@link Authentication} with a fresh {@link RydenUserDetails} whose
     * {@code forename}/{@code surname} match the provided values, then persists the new
     * {@link SecurityContext} through {@link SecurityContextRepository#saveContext}. No-op when
     * there is no authenticated user or the principal is not a {@link RydenUserDetails} instance.
     */
    public void refreshDisplayName(
            final String forename,
            final String surname,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof RydenUserDetails old)) {
            return;
        }
        final RydenUserDetails updated = new RydenUserDetails(
                old.getUserId(),
                old.getUsername(),
                forename,
                surname,
                old.getPassword(),
                old.getAuthorities(),
                old.getRoleAssignedBy().orElse(null));
        final UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                updated,
                authentication.getCredentials(),
                updated.getAuthorities());
        newAuth.setDetails(authentication.getDetails());
        final SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(newAuth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
