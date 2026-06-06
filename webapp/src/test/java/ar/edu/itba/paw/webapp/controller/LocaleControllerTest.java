package ar.edu.itba.paw.webapp.controller;

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

import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.util.rules.SupportedLocales;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.i18n.RydenLocaleResolver;

/**
 * Contract tests for {@link LocaleController}: assert observable outcomes only — the cookie written to
 * the HTTP response and the {@code latest_locale} that ended up being persisted. Mockito {@code verify}
 * (or any "was-it-called" check) is intentionally avoided: behaviour, not interactions.
 *
 * <p>{@link UserService} is a Mockito mock so this test does not have to be re-edited every time the
 * service contract grows. The only stub is an {@code Answer} that captures the {@code updateLatestLocale}
 * argument into {@link #persistedLocaleByUser}, which the assertions then read like any other piece of
 * state.</p>
 *
 * <p>The controller now receives a {@link java.util.Locale} (already bound by Spring via the
 * {@code StringToSupportedLocaleConverter}, which yields {@code null} for unsupported / blank tags).
 * These tests therefore feed in {@link java.util.Locale} instances — or {@code null} when simulating
 * the "silently ignore" path the converter takes for unknown input.</p>
 */
@ExtendWith(MockitoExtension.class)
class LocaleControllerTest {

    private static final long USER_ID = 77L;
    private static final String COOKIE_NAME = RydenLocaleResolver.COOKIE_NAME;

    @Mock
    private UserService userService;

    /** Captures every {@code updateLatestLocale(userId, tag)} call so tests can assert on the
     *  final state instead of on interactions. */
    private final Map<Long, String> persistedLocaleByUser = new HashMap<>();

    private LocaleResolver localeResolver;
    private LocaleController controller;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        persistedLocaleByUser.clear();
        // lenient() so tests that never trigger the locale persistence path (anonymous visitor,
        // blank/unsupported lang) do not trip Mockito's strict-stub check.
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
        // 1.Arrange
        final User signedInUser = userWithId(USER_ID);

        // 2.Act
        final ModelAndView result = controller.changeLocale(
                SupportedLocales.SPANISH, "https://example.test/search", signedInUser, request, response);

        // 3.Assert
        assertCookieValue("es");
        assertEquals("es", persistedLocaleByUser.get(USER_ID));
        assertRedirectsTo(result, "https://example.test/search");
    }

    @Test
    void testChangeLocaleToEnglishPersistsCookieButLeavesLatestLocaleUntouchedForAnonymousVisitor() {
        // 1.Arrange — anonymous visitor (no User passed in).

        // 2.Act
        final ModelAndView result = controller.changeLocale(
                SupportedLocales.ENGLISH, "/login", null, request, response);

        // 3.Assert
        assertCookieValue("en");
        assertTrue(persistedLocaleByUser.isEmpty(),
                "Anonymous toggle must not persist latest_locale on any user");
        assertRedirectsTo(result, "/login");
    }

    @Test
    void testChangeLocaleLeavesCookieAndLatestLocaleUntouchedWhenLanguageIsUnsupported() {
        // 1.Arrange — Spring's StringToSupportedLocaleConverter returns null for unsupported tags
        // (e.g. "fr"), so the controller receives null and must skip cookie + latest_locale writes.
        final User signedInUser = userWithId(USER_ID);

        // 2.Act
        final ModelAndView result = controller.changeLocale(
                null, "/", signedInUser, request, response);

        // 3.Assert
        assertNoCookieWritten();
        assertTrue(persistedLocaleByUser.isEmpty(),
                "Unsupported language must be silently ignored");
        assertRedirectsTo(result, "/");
    }

    @Test
    void changeLocaleLeavesCookieAndLatestLocaleUntouchedWhenLanguageIsBlank() {
        // 1.Arrange — blank input is mapped to null by the converter; controller treats it the same
        // as an unsupported tag.

        // 2.Act
        final ModelAndView result = controller.changeLocale(
                null, "/search", null, request, response);

        // 3.Assert
        assertNoCookieWritten();
        assertTrue(persistedLocaleByUser.isEmpty());
        assertRedirectsTo(result, "/search");
    }

    @Test
    void testChangeLocaleRedirectsToHomeWhenRefererMissing() {
        // 1.Arrange / 2.Act
        final ModelAndView result = controller.changeLocale(
                SupportedLocales.SPANISH, null, null, request, response);

        // 3.Assert
        assertCookieValue("es");
        assertRedirectsTo(result, "/");
    }

    @Test
    void changeLocaleRedirectsToHomeWhenRefererIsBlank() {
        // 1.Arrange / 2.Act
        final ModelAndView result = controller.changeLocale(
                SupportedLocales.ENGLISH, "", null, request, response);

        // 3.Assert
        assertCookieValue("en");
        assertRedirectsTo(result, "/");
    }

    // ----- Helpers --------------------------------------------------------------------------

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
        assertNotNull(cookie, "Expected the resolver to write a " + COOKIE_NAME + " cookie");
        assertEquals(expectedLanguageTag, cookie.getValue());
    }

    private void assertNoCookieWritten() {
        final Cookie cookie = response.getCookie(COOKIE_NAME);
        assertNull(cookie, "No cookie must be written when the language is invalid");
    }

    private static void assertRedirectsTo(final ModelAndView mv, final String expectedUrl) {
        assertNotNull(mv);
        final RedirectView view = assertInstanceOf(RedirectView.class, mv.getView());
        assertEquals(expectedUrl, view.getUrl());
    }

    @Test
    void testCookieWrittenForSpanishIsExactlyTheBcp47Tag() {
        // 1.Arrange — sanity check: the cookie value the browser will see is the BCP 47 tag, not the
        // toString() of the Locale (which would be locale-dependent and would leak region/variant).

        // 2.Act
        controller.changeLocale(SupportedLocales.SPANISH, "/", null, request, response);

        // 3.Assert
        final Cookie cookie = response.getCookie(COOKIE_NAME);
        assertNotNull(cookie);
        assertFalse(cookie.getValue().contains("_"),
                "Cookie value must be a BCP 47 tag (no underscores)");
        assertEquals("es", cookie.getValue());
    }
}
