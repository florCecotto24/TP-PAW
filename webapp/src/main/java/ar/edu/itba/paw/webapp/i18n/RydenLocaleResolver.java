package ar.edu.itba.paw.webapp.i18n;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import ar.edu.itba.paw.models.util.rules.SupportedLocales;
import ar.edu.itba.paw.services.user.UserLocaleService;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * UI locale resolver. Priority (Accept-Language is intentionally ignored — the user picks the language explicitly):
 * <ol>
 *   <li>The signed-in user's {@code latest_locale} when present and supported.</li>
 *   <li>The {@code RYDEN_LOCALE} cookie set by the language toggle (anonymous visitors).</li>
 *   <li>Spanish as the application default.</li>
 * </ol>
 * {@link #setLocale(HttpServletRequest, HttpServletResponse, Locale)} writes the cookie for anonymous visitors;
 * persisting the choice into {@code latest_locale} happens at the controller layer.
 */
public final class RydenLocaleResolver {

    public static final String COOKIE_NAME = "RYDEN_LOCALE";
    private static final int COOKIE_MAX_AGE_SECONDS = (int) Duration.ofDays(365).toSeconds();
    private static final String REQUEST_CACHE_ATTR = RydenLocaleResolver.class.getName() + ".RESOLVED";

    private final UserLocaleService userLocaleService;

    public RydenLocaleResolver(final UserLocaleService userLocaleService) {
        this.userLocaleService = Objects.requireNonNull(userLocaleService, "userLocaleService");
    }

    @NonNull
    public Locale resolveLocale(@NonNull final HttpServletRequest request) {
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
                .flatMap(userLocaleService::findUserPreferredLocale);
        if (fromUser.isPresent()) {
            return fromUser.get();
        }
        final Locale fromCookie = readCookieLocale(request);
        return SupportedLocales.ALL.contains(fromCookie) ? fromCookie : SupportedLocales.DEFAULT;
    }

    public void setLocale(
            @NonNull final HttpServletRequest request,
            final HttpServletResponse response,
            final Locale locale) {
        request.removeAttribute(REQUEST_CACHE_ATTR);
        if (response == null) {
            return;
        }
        final Cookie cookie = new Cookie(COOKIE_NAME, locale != null ? locale.toLanguageTag() : "");
        cookie.setPath("/");
        cookie.setMaxAge(locale != null ? COOKIE_MAX_AGE_SECONDS : 0);
        response.addCookie(cookie);
    }

    private static Locale readCookieLocale(final HttpServletRequest request) {
        final Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return SupportedLocales.DEFAULT;
        }
        for (final Cookie cookie : cookies) {
            if (!COOKIE_NAME.equals(cookie.getName())) {
                continue;
            }
            final String value = cookie.getValue();
            if (value == null || value.isBlank()) {
                return SupportedLocales.DEFAULT;
            }
            return Locale.forLanguageTag(value);
        }
        return SupportedLocales.DEFAULT;
    }

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
