package ar.edu.itba.paw.webapp.security;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.services.UserService;

@Component
public class SessionLoginService {

    private final UserService userService;
    private final SecurityContextRepository securityContextRepository;

    @Autowired
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
        final String hash = withHash.getPasswordHash().filter(h -> !h.isBlank())
                .orElseThrow(() -> new IllegalStateException("User has no password after verification"));
        final List<GrantedAuthority> authorities =
                UserRoleAuthorities.fromDbRoleNames(userService.findRoleNamesForUser(withHash.getId()));
        final RydenUserDetails principal = new RydenUserDetails(
                withHash.getId(),
                withHash.getEmail(),
                withHash.getForename(),
                withHash.getSurname(),
                hash,
                authorities);
        final UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        final SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        this.securityContextRepository.saveContext(context, request, response);
    }
}
