package ar.edu.itba.paw.webapp.i18n;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import ar.edu.itba.paw.models.util.rules.SupportedLocales;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * UI locale resolver. Priority (Accept-Language is intentionally ignored — the user picks the language explicitly):
 * <ol>
 *   <li>The signed-in user's {@code latest_locale} when present and supported.</li>
 *   <li>The {@code RYDEN_LOCALE} cookie set by the language toggle (anonymous visitors).</li>
 *   <li>Spanish as the application default.</li>
 * </ol>
 * {@link #setLocale(HttpServletRequest, javax.servlet.http.HttpServletResponse, Locale)} delegates to the cookie
 * (anonymous + first-time visitors); persisting the choice into {@code latest_locale} happens at the controller layer
 * so the resolver stays a single-responsibility view-layer component.
 */
public final class RydenLocaleResolver extends CookieLocaleResolver {

    public static final String COOKIE_NAME = "RYDEN_LOCALE";
    private static final int COOKIE_MAX_AGE_SECONDS = (int) Duration.ofDays(365).toSeconds();
    private static final String REQUEST_CACHE_ATTR = RydenLocaleResolver.class.getName() + ".RESOLVED";

    private final UserService userService;

    public RydenLocaleResolver(final UserService userService) {
        this.userService = Objects.requireNonNull(userService, "userService");
        setCookieName(COOKIE_NAME);
        setCookieMaxAge(COOKIE_MAX_AGE_SECONDS);
        setCookiePath("/");
        setDefaultLocale(SupportedLocales.DEFAULT);
        setLanguageTagCompliant(true);
    }

    @Override
    @NonNull
    public Locale resolveLocale(@NonNull final HttpServletRequest request) {
        // Spring's view layer (interceptors, JSP tags, controller advice, security entry points) calls
        // resolveLocale several times per request. The user-id branch performs a SELECT on users to read
        // {@code latest_locale}; without caching, a single page render issued one extra DB query per
        // resolveLocale() call. Memoize on the request scope so each HTTP request hits the database at
        // most once (request attributes are inherently per-thread and per-request, so this is safe).
        final Object cached = request.getAttribute(REQUEST_CACHE_ATTR);
        if (cached instanceof Locale locale) {
            return locale;
        }
        final Locale resolved = doResolveLocale(request);
        request.setAttribute(REQUEST_CACHE_ATTR, resolved);
        return resolved;
    }

    private Locale doResolveLocale(final HttpServletRequest request) {
        final Optional<Locale> fromUser = currentAuthenticatedUserId()
                .flatMap(userService::findUserPreferredLocale);
        if (fromUser.isPresent()) {
            return fromUser.get();
        }
        final Locale fromCookie = super.resolveLocale(request);
        return SupportedLocales.ALL.contains(fromCookie) ? fromCookie : SupportedLocales.DEFAULT;
    }

    @Override
    public void setLocale(
            @NonNull final HttpServletRequest request,
            final javax.servlet.http.HttpServletResponse response,
            final Locale locale) {
        // Invalidate the per-request memoization when the locale toggle writes a new cookie mid-request,
        // otherwise the rest of the rendering pipeline would still see the previous value.
        request.removeAttribute(REQUEST_CACHE_ATTR);
        super.setLocale(request, response, locale);
    }

    /**
     * Resolves the current authenticated user's id from the security context, or empty when the request is anonymous.
     */
    private static Optional<Long> currentAuthenticatedUserId() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        if (auth.getPrincipal() instanceof RydenUserDetails principal) {
            return Optional.of(principal.getUserId());
        }
        return Optional.empty();
    }
}
