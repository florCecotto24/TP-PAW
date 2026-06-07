package ar.edu.itba.paw.webapp.controller;

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

import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.support.CurrentUser;
import ar.edu.itba.paw.webapp.support.SafeRefererResolver;
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
     *
     * <p>The {@code Referer} target is validated as same-application (host/scheme/port match) to
     * avoid an open-redirect when an attacker forges a Referer header pointing elsewhere.</p>
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
        final String target = SafeRefererResolver.sameAppRelativePathOrDefault(request, referer, "/");
        return new ModelAndView(new RedirectView(target, true));
    }
}
