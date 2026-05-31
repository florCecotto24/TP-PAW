package ar.edu.itba.paw.webapp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.util.SupportedLocales;
import ar.edu.itba.paw.services.UserService;

/**
 * Locale toggle contract tests. The endpoint must (a) persist the cookie via the resolver,
 * (b) update {@code latest_locale} only when signed in, and (c) bounce the user back to the referer
 * (or home when missing/invalid) without leaking open-redirect targets.
 */
@ExtendWith(MockitoExtension.class)
class LocaleControllerTest {

    private static final long USER_ID = 77L;

    @Mock private LocaleResolver localeResolver;
    @Mock private UserService userService;

    private LocaleController controller;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        controller = new LocaleController(localeResolver, userService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void changeLocaleToSpanishPersistsCookieAndUpdatesLatestLocaleForSignedInUser() {
        final User currentUser = userWithId(USER_ID);

        final ModelAndView result = controller.changeLocale(
                "es", "https://example.test/search", currentUser, request, response);

        verify(localeResolver).setLocale(eq(request), eq(response), eq(SupportedLocales.SPANISH));
        verify(userService).updateLatestLocale(USER_ID, "es");
        assertRedirectsTo(result, "https://example.test/search");
    }

    @Test
    void changeLocaleToEnglishPersistsCookieButSkipsLatestLocaleForAnonymousVisitor() {
        final ModelAndView result = controller.changeLocale(
                "en", "/login", null, request, response);

        verify(localeResolver).setLocale(eq(request), eq(response), eq(SupportedLocales.ENGLISH));
        verify(userService, never()).updateLatestLocale(anyLong(), anyString());
        assertRedirectsTo(result, "/login");
    }

    @Test
    void changeLocaleIsNoOpWhenLanguageIsUnsupported() {
        final User currentUser = userWithId(USER_ID);

        final ModelAndView result = controller.changeLocale(
                "fr", "/", currentUser, request, response);

        verifyNoInteractions(localeResolver);
        verify(userService, never()).updateLatestLocale(anyLong(), anyString());
        assertRedirectsTo(result, "/");
    }

    @Test
    void changeLocaleIsNoOpWhenLanguageIsBlank() {
        final ModelAndView result = controller.changeLocale(
                "   ", "/search", null, request, response);

        verifyNoInteractions(localeResolver);
        verifyNoInteractions(userService);
        assertRedirectsTo(result, "/search");
    }

    @Test
    void changeLocaleRedirectsToHomeWhenRefererMissing() {
        final ModelAndView result = controller.changeLocale(
                "es", null, null, request, response);

        verify(localeResolver).setLocale(any(), any(), eq(SupportedLocales.SPANISH));
        assertRedirectsTo(result, "/");
    }

    @Test
    void changeLocaleRedirectsToHomeWhenRefererIsBlank() {
        final ModelAndView result = controller.changeLocale(
                "en", "", null, request, response);

        verify(localeResolver).setLocale(any(), any(), eq(SupportedLocales.ENGLISH));
        assertRedirectsTo(result, "/");
    }

    private static User userWithId(final long id) {
        return User.builder()
                .id(id)
                .email("user@example.com")
                .forename("Test")
                .surname("User")
                .build();
    }

    private static void assertRedirectsTo(final ModelAndView mv, final String expectedUrl) {
        assertNotNull(mv);
        final RedirectView view = assertInstanceOf(RedirectView.class, mv.getView());
        assertEquals(expectedUrl, view.getUrl());
    }
}
