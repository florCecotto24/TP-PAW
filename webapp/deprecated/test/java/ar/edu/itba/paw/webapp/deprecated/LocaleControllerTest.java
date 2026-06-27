package ar.edu.itba.paw.webapp.deprecated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.view.RedirectView;

import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.util.rules.SupportedLocales;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.deprecated.mvc.controller.LocaleController;
import ar.edu.itba.paw.webapp.i18n.RydenLocaleResolver;

/**
 * Legacy MVC test for {@link LocaleController} — not compiled by Maven (lives under {@code deprecated/test}).
 */
@ExtendWith(MockitoExtension.class)
class LocaleControllerTest {

    private static final long USER_ID = 77L;
    private static final String COOKIE_NAME = RydenLocaleResolver.COOKIE_NAME;

    @Mock
    private UserService userService;

    private final Map<Long, String> persistedLocaleByUser = new HashMap<>();

    private LocaleResolver localeResolver;
    private LocaleController controller;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        persistedLocaleByUser.clear();
        lenient().doAnswer(invocation -> {
            persistedLocaleByUser.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(userService).updateLatestLocale(anyLong(), anyString());

        final CookieLocaleResolver cookieResolver = new CookieLocaleResolver();
        cookieResolver.setCookieName(COOKIE_NAME);
        cookieResolver.setDefaultLocale(SupportedLocales.DEFAULT);
        cookieResolver.setLanguageTagCompliant(true);
        localeResolver = cookieResolver;
        controller = new LocaleController(localeResolver, userService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void changeLocaleToSpanishPersistsCookieAndUpdatesLatestLocaleForSignedInUser() {
        final User signedInUser = userWithId(USER_ID);
        final ModelAndView result = controller.changeLocale(
                SupportedLocales.SPANISH, "http://localhost/search", signedInUser, request, response);
        assertCookieValue("es");
        assertEquals("es", persistedLocaleByUser.get(USER_ID));
        assertRedirectsTo(result, "/search");
    }

    @Test
    void testChangeLocaleToEnglishPersistsCookieButLeavesLatestLocaleUntouchedForAnonymousVisitor() {
        final ModelAndView result = controller.changeLocale(
                SupportedLocales.ENGLISH, "http://localhost/login", null, request, response);
        assertCookieValue("en");
        assertTrue(persistedLocaleByUser.isEmpty());
        assertRedirectsTo(result, "/login");
    }

    @Test
    void testChangeLocaleLeavesCookieAndLatestLocaleUntouchedWhenLanguageIsUnsupported() {
        final User signedInUser = userWithId(USER_ID);
        final ModelAndView result = controller.changeLocale(null, "/", signedInUser, request, response);
        assertNoCookieWritten();
        assertTrue(persistedLocaleByUser.isEmpty());
        assertRedirectsTo(result, "/");
    }

    @Test
    void changeLocaleLeavesCookieAndLatestLocaleUntouchedWhenLanguageIsBlank() {
        final ModelAndView result = controller.changeLocale(
                null, "http://localhost/search", null, request, response);
        assertNoCookieWritten();
        assertTrue(persistedLocaleByUser.isEmpty());
        assertRedirectsTo(result, "/search");
    }

    @Test
    void testChangeLocaleRedirectsToHomeWhenRefererMissing() {
        final ModelAndView result = controller.changeLocale(
                SupportedLocales.SPANISH, null, null, request, response);
        assertCookieValue("es");
        assertRedirectsTo(result, "/");
    }

    @Test
    void changeLocaleRedirectsToHomeWhenRefererIsBlank() {
        final ModelAndView result = controller.changeLocale(
                SupportedLocales.ENGLISH, "", null, request, response);
        assertCookieValue("en");
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

    private void assertCookieValue(final String expectedLanguageTag) {
        final Cookie cookie = response.getCookie(COOKIE_NAME);
        assertNotNull(cookie);
        assertEquals(expectedLanguageTag, cookie.getValue());
    }

    private void assertNoCookieWritten() {
        assertNull(response.getCookie(COOKIE_NAME));
    }

    private static void assertRedirectsTo(final ModelAndView mv, final String expectedUrl) {
        assertNotNull(mv);
        final RedirectView view = assertInstanceOf(RedirectView.class, mv.getView());
        assertEquals(expectedUrl, view.getUrl());
    }

    @Test
    void testCookieWrittenForSpanishIsExactlyTheBcp47Tag() {
        controller.changeLocale(SupportedLocales.SPANISH, "/", null, request, response);
        final Cookie cookie = response.getCookie(COOKIE_NAME);
        assertNotNull(cookie);
        assertFalse(cookie.getValue().contains("_"));
        assertEquals("es", cookie.getValue());
    }
}
