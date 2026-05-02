package ar.edu.itba.paw.webapp.interceptor;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * After each request, persists the resolved MVC locale for signed-in users (not anonymous) for mail and defaults.
 */
@Component
public final class LatestLocaleSaveInterceptor implements HandlerInterceptor {

    private final UserService userService;

    public LatestLocaleSaveInterceptor(final UserService userService) {
        this.userService = userService;
    }

    @Override
    public void afterCompletion(
            @NonNull final HttpServletRequest request,
            @NonNull final HttpServletResponse response,
            @NonNull final Object handler,
            final Exception ex) {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return;
        }
        if (!(auth.getPrincipal() instanceof RydenUserDetails)) {
            return;
        }
        final RydenUserDetails principal = (RydenUserDetails) auth.getPrincipal();
        final Locale locale = RequestContextUtils.getLocale(request);
        if (locale == null) {
            return;
        }
        final String tag = locale.toLanguageTag();
        if (tag.isBlank()) {
            return;
        }
        userService.updateLatestLocale(principal.getUserId(), tag);
    }
}
