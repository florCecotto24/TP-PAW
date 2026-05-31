package ar.edu.itba.paw.webapp.controller;

import java.net.URI;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.support.converter.StringToSupportedLocaleConverter;

/**
 * Handles the navbar language toggle. Persists the chosen locale into the cookie used by the resolver and,
 * when the user is signed in, into their {@code latest_locale} so future requests and async mail keep the
 * preference. Unsupported {@code lang} values are silently ignored (the resolver falls back to the default).
 */
@Controller
@RequestMapping("/i18n")
public final class LocaleController {

    private final LocaleResolver localeResolver;
    private final UserService userService;

    public LocaleController(final LocaleResolver localeResolver, final UserService userService) {
        this.localeResolver = localeResolver;
        this.userService = userService;
    }

    /**
     * Spring binds {@code lang} into a {@link Locale} via {@link StringToSupportedLocaleConverter},
     * which mirrors the previous {@code SupportedLocales.parse(raw)} call and returns {@code null}
     * for unknown / unsupported tags so this endpoint keeps the "silently ignore" contract.
     */
    @PostMapping("/locale")
    public ModelAndView changeLocale(
            @RequestParam(name = "lang", required = false) final Locale lang,
            @RequestHeader(name = "Referer", required = false) final String referer,
            @CurrentUser final User currentUser,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        if (lang != null) {
            localeResolver.setLocale(request, response, lang);
            if (currentUser != null) {
                userService.updateLatestLocale(currentUser.getId(), lang.toLanguageTag());
            }
        }
        return new ModelAndView(new RedirectView(resolveRedirectTarget(referer), false));
    }

    /**
     * Bounce the visitor back to where they came from; default to home when unknown or invalid.
     * The referer URL is kept opaque (we only validate that it parses as a URI) to avoid open-redirect leaks.
     */
    private static String resolveRedirectTarget(final String referer) {
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            return URI.create(referer).toString();
        } catch (final IllegalArgumentException e) {
            return "/";
        }
    }
}
