package ar.edu.itba.paw.webapp.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import ar.edu.itba.paw.models.util.rules.SupportedLocales;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

@ExtendWith(MockitoExtension.class)
class RydenLocaleResolverTest {

    private static final long USER_ID = 42L;

    @Mock private UserService userService;

    private RydenLocaleResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new RydenLocaleResolver(userService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolveLocaleReturnsSpanishByDefaultForAnonymousUserWithoutCookie() {
        // 1.Arrange
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken(
                        "anon",
                        "anonymousUser",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        final MockHttpServletRequest request = new MockHttpServletRequest();

        // 2.Act
        final Locale resolved = resolver.resolveLocale(request);

        // 3.Assert
        assertEquals(SupportedLocales.SPANISH, resolved);
    }

    @Test
    void resolveLocaleIgnoresAcceptLanguageHeaderForAnonymousUser() {
        // 1.Arrange
        SecurityContextHolder.clearContext();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "en-US,en;q=0.9");

        // 2.Act
        final Locale resolved = resolver.resolveLocale(request);

        // 3.Assert
        assertEquals(SupportedLocales.SPANISH, resolved,
                "Accept-Language must be ignored; default Spanish should win for anonymous visitors");
    }

    @Test
    void resolveLocaleReadsCookieWhenSetByLanguageToggleForAnonymousUser() {
        // 1.Arrange
        SecurityContextHolder.clearContext();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(RydenLocaleResolver.COOKIE_NAME, "en"));

        // 2.Act
        final Locale resolved = resolver.resolveLocale(request);

        // 3.Assert
        assertEquals(SupportedLocales.ENGLISH, resolved);
    }

    @Test
    void resolveLocaleFallsBackToDefaultWhenCookieValueUnsupported() {
        // 1.Arrange
        SecurityContextHolder.clearContext();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(RydenLocaleResolver.COOKIE_NAME, "fr"));
        lenient().when(userService.findUserPreferredLocale(anyLong())).thenReturn(Optional.empty());

        // 2.Act
        final Locale resolved = resolver.resolveLocale(request);

        // 3.Assert
        assertEquals(SupportedLocales.SPANISH, resolved);
    }

    @Test
    void resolveLocaleHonoursAuthenticatedUserPreferenceOverCookie() {
        // 1.Arrange
        authenticateAs(USER_ID);
        when(userService.findUserPreferredLocale(USER_ID)).thenReturn(Optional.of(SupportedLocales.ENGLISH));
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(RydenLocaleResolver.COOKIE_NAME, "es"));

        // 2.Act
        final Locale resolved = resolver.resolveLocale(request);

        // 3.Assert
        assertEquals(SupportedLocales.ENGLISH, resolved,
                "When the signed-in user has a stored preference it must win over the cookie");
    }

    @Test
    void resolveLocaleFallsThroughToCookieWhenAuthenticatedUserHasNoStoredPreference() {
        // 1.Arrange
        authenticateAs(USER_ID);
        when(userService.findUserPreferredLocale(USER_ID)).thenReturn(Optional.empty());
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(RydenLocaleResolver.COOKIE_NAME, "en"));

        // 2.Act
        final Locale resolved = resolver.resolveLocale(request);

        // 3.Assert
        assertEquals(SupportedLocales.ENGLISH, resolved);
    }

    private static void authenticateAs(final long userId) {
        final RydenUserDetails principal = RydenUserDetails.builder()
                .userId(userId)
                .email("test@example.com")
                .forename("Test")
                .surname("User")
                .encodedPassword("x")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "x", principal.getAuthorities()));
    }
}
