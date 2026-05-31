package ar.edu.itba.paw.webapp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.view.RedirectView;

import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.domain.UserDocumentType;
import ar.edu.itba.paw.models.util.SupportedLocales;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.i18n.RydenLocaleResolver;

/**
 * Contract tests for {@link LocaleController}: assert observable outcomes only — the cookie written to
 * the HTTP response and the {@code latest_locale} captured by an in-memory fake {@link UserService}.
 * Mockito {@code verify} (or any "was-it-called" check) is intentionally avoided: behaviour, not
 * interactions.
 */
class LocaleControllerTest {

    private static final long USER_ID = 77L;
    private static final String COOKIE_NAME = RydenLocaleResolver.COOKIE_NAME;

    private RecordingUserService userService;
    private LocaleResolver localeResolver;
    private LocaleController controller;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        userService = new RecordingUserService();
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
                "es", "https://example.test/search", signedInUser, request, response);

        // 3.Assert
        assertCookieValue("es");
        assertEquals("es", userService.latestUpdateFor(USER_ID));
        assertRedirectsTo(result, "https://example.test/search");
    }

    @Test
    void testChangeLocaleToEnglishPersistsCookieButLeavesLatestLocaleUntouchedForAnonymousVisitor() {
        // 1.Arrange — anonymous visitor (no User passed in).

        // 2.Act
        final ModelAndView result = controller.changeLocale(
                "en", "/login", null, request, response);

        // 3.Assert
        assertCookieValue("en");
        assertTrue(userService.updates.isEmpty(),
                "Anonymous toggle must not persist latest_locale on any user");
        assertRedirectsTo(result, "/login");
    }

    @Test
    void testChangeLocaleLeavesCookieAndLatestLocaleUntouchedWhenLanguageIsUnsupported() {
        // 1.Arrange
        final User signedInUser = userWithId(USER_ID);

        // 2.Act
        final ModelAndView result = controller.changeLocale(
                "fr", "/", signedInUser, request, response);

        // 3.Assert
        assertNoCookieWritten();
        assertTrue(userService.updates.isEmpty(),
                "Unsupported language must be silently ignored");
        assertRedirectsTo(result, "/");
    }

    @Test
    void changeLocaleLeavesCookieAndLatestLocaleUntouchedWhenLanguageIsBlank() {
        // 1.Arrange — no signed-in user, blank lang.

        // 2.Act
        final ModelAndView result = controller.changeLocale(
                "   ", "/search", null, request, response);

        // 3.Assert
        assertNoCookieWritten();
        assertTrue(userService.updates.isEmpty());
        assertRedirectsTo(result, "/search");
    }

    @Test
    void testChangeLocaleRedirectsToHomeWhenRefererMissing() {
        // 1.Arrange / 2.Act
        final ModelAndView result = controller.changeLocale(
                "es", null, null, request, response);

        // 3.Assert
        assertCookieValue("es");
        assertRedirectsTo(result, "/");
    }

    @Test
    void changeLocaleRedirectsToHomeWhenRefererIsBlank() {
        // 1.Arrange / 2.Act
        final ModelAndView result = controller.changeLocale(
                "en", "", null, request, response);

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

    /**
     * Test double that records every {@code updateLatestLocale} call into its own state, exposing it for
     * assertions. Unused methods throw — they would indicate a regression in {@link LocaleController}
     * pulling in dependencies it has no business with.
     */
    private static final class RecordingUserService implements UserService {

        final Map<Long, String> updates = new HashMap<>();

        String latestUpdateFor(final long userId) {
            return updates.get(userId);
        }

        @Override
        public void updateLatestLocale(final long userId, final String localeTag) {
            updates.put(userId, localeTag);
        }

        // ----- Not exercised by the SUT for these tests; fail loudly if invoked. -----
        private static UnsupportedOperationException notNeeded() {
            return new UnsupportedOperationException(
                    "LocaleController should not call this UserService method");
        }

        @Override public User registerUser(String e, String f, String s, String p, String pc)              { throw notNeeded(); }
        @Override public Optional<User> findByEmail(String email)                                           { throw notNeeded(); }
        @Override public void markEmailVerified(long userId)                                                { throw notNeeded(); }
        @Override public Optional<User> getUserById(long id)                                                { throw notNeeded(); }
        @Override public Optional<User> findByEmailForAuthentication(String email)                          { throw notNeeded(); }
        @Override public List<String> findRoleNamesForUser(long userId)                                     { throw notNeeded(); }
        @Override public void updateDisplayName(long userId, String f, String s)                            { throw notNeeded(); }
        @Override public void updatePhoneNumber(long userId, String phoneRaw)                               { throw notNeeded(); }
        @Override public void updateBirthDate(long userId, LocalDate birthDate)                             { throw notNeeded(); }
        @Override public void updateAbout(long userId, String aboutRaw)                                     { throw notNeeded(); }
        @Override public void updateProfilePicture(long userId, String fn, String ct, byte[] data)          { throw notNeeded(); }
        @Override public void clearProfilePicture(long userId)                                              { throw notNeeded(); }
        @Override public void uploadValidatedProfileDocument(long userId, UserDocumentType d, String fn,
                                                              String ct, byte[] data)                       { throw notNeeded(); }
        @Override public void clearProfileDocument(long userId, UserDocumentType documentType)              { throw notNeeded(); }
        @Override public void changePassword(long userId, String cur, String n, String nc)                  { throw notNeeded(); }
        @Override public void replacePasswordHash(long userId, String hash)                                 { throw notNeeded(); }
        @Override public void assignRandomPasswordAndEmailForLegacyUser(long userId, String e, Locale l)    { throw notNeeded(); }
        @Override public Optional<Locale> findUserPreferredLocale(long userId)                              { throw notNeeded(); }
        @Override public Locale resolveMailLocale(long userId)                                              { throw notNeeded(); }
        @Override public Locale resolveMailLocaleOrElse(long userId, Locale fallback)                       { throw notNeeded(); }
        @Override public void updateCbu(long userId, String cbu)                                            { throw notNeeded(); }
        @Override public String getUserCbu(long userId)                                                     { throw notNeeded(); }
        @Override public Optional<String> findOwnerCbuForCar(long carId)                                    { throw notNeeded(); }
        @Override public boolean hasValidCbu(User user)                                                     { throw notNeeded(); }
        @Override public boolean hasUploadedLicenseAndIdentity(User user)                                   { throw notNeeded(); }
        @Override public boolean isValidCbuFormat(String cbuRaw)                                            { throw notNeeded(); }
        @Override public User registerUserRequiringAccountConfirmation(String e, String f, String s,
                                                                        String p, String pc, Locale l)      { throw notNeeded(); }
        @Override public void ensureAccountConfirmationPrerequisites(String email, Locale locale)           { throw notNeeded(); }
        @Override public boolean requestAccountConfirmationResend(String email, Locale locale)              { throw notNeeded(); }
        @Override public long completeAccountConfirmation(String email, String code)                       { throw notNeeded(); }
        @Override public void blockUser(long userId)                                                        { throw notNeeded(); }
        @Override public void unblockUser(long userId)                                                      { throw notNeeded(); }
    }

    @Test
    void testCookieWrittenForSpanishIsExactlyTheBcp47Tag() {
        // 1.Arrange — sanity check: the cookie value the browser will see is the BCP 47 tag, not the
        // toString() of the Locale (which would be locale-dependent and would leak region/variant).

        // 2.Act
        controller.changeLocale("es", "/", null, request, response);

        // 3.Assert
        final Cookie cookie = response.getCookie(COOKIE_NAME);
        assertNotNull(cookie);
        assertFalse(cookie.getValue().contains("_"),
                "Cookie value must be a BCP 47 tag (no underscores)");
        assertEquals("es", cookie.getValue());
    }
}
