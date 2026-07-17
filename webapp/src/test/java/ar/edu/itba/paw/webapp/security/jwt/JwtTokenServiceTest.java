package ar.edu.itba.paw.webapp.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import ar.edu.itba.paw.webapp.config.properties.AppSecurityJwtProperties;
import ar.edu.itba.paw.webapp.security.auth.RydenAuthorities;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

class JwtTokenServiceTest {

    private static final String SECRET = "unit-test-secret-value-please-change-me-long-enough";

    private static RydenUserDetails principal() {
        return principal(false);
    }

    private static RydenUserDetails principal(final boolean passwordResetOtp) {
        final Set<GrantedAuthority> authorities = passwordResetOtp
                ? Set.of(new SimpleGrantedAuthority(RydenAuthorities.PASSWORD_RESET_OTP))
                : Set.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN"));
        return RydenUserDetails.builder()
                .userId(42L)
                .email("ana@example.com")
                .forename("Ana")
                .surname("Beltran")
                .encodedPassword("irrelevant")
                .authorities(authorities)
                .roleAssignedBy(7L)
                .build();
    }

    private static JwtTokenService service(final String secret, final int accessMin, final int refreshDays) {
        final AppSecurityJwtProperties props =
                new AppSecurityJwtProperties(secret, "ryden", accessMin, refreshDays);
        final MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        return new JwtTokenService(props, environment);
    }

    private static MockHttpServletResponse issue(final JwtTokenService service, final RydenUserDetails principal) {
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/webapp");
        request.setRequestURI("/webapp/api/");
        service.attachTokenHeaders(response, principal, request);
        return response;
    }

    @Test
    void testIssuesAndParsesAccessTokenWithClaims() {
        // 1.Arrange
        final JwtTokenService jwt = service(SECRET, 15, 30);
        final MockHttpServletResponse response = issue(jwt, principal());

        // 2.Act
        final TokenService.ParsedJwt parsed = jwt.parseToken(response.getHeader(JwtTokenService.ACCESS_TOKEN_HEADER));

        // 3.Assert
        assertNotNull(parsed);
        assertEquals(JwtTokenType.ACCESS, parsed.type());
        assertEquals(42L, parsed.principal().getUserId());
        assertEquals("ana@example.com", parsed.principal().getUsername());
        assertNull(parsed.principal().getForename());
        assertNull(parsed.principal().getSurname());
        assertTrue(parsed.principal().getRoleAssignedBy().isEmpty());
        assertTrue(parsed.principal().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void testNormalLoginRefreshDoesNotCarryPasswordResetOtp() {
        // 1.Arrange / 2.Act
        final JwtTokenService jwt = service(SECRET, 15, 30);
        final MockHttpServletResponse response = issue(jwt, principal(false));
        final TokenService.ParsedJwt access = jwt.parseToken(response.getHeader(JwtTokenService.ACCESS_TOKEN_HEADER));
        final TokenService.ParsedJwt refresh = jwt.parseToken(response.getHeader(JwtTokenService.REFRESH_TOKEN_HEADER));

        // 3.Assert
        assertNotNull(access);
        assertNotNull(refresh);
        assertFalse(hasPasswordResetOtp(access.principal()));
        assertFalse(hasPasswordResetOtp(refresh.principal()));
    }

    @Test
    void testPasswordResetOtpIsAccessOnlyNeverEmitsRefresh() {
        // 1.Arrange / 2.Act
        final JwtTokenService jwt = service(SECRET, 15, 30);
        final MockHttpServletResponse response = issue(jwt, principal(true));
        final TokenService.ParsedJwt access = jwt.parseToken(response.getHeader(JwtTokenService.ACCESS_TOKEN_HEADER));
        final String refreshHeader = response.getHeader(JwtTokenService.REFRESH_TOKEN_HEADER);

        // 3.Assert
        assertNotNull(access);
        assertNull(refreshHeader);
        assertTrue(hasPasswordResetOtp(access.principal()));
        assertTrue(access.principal().getAuthorities().stream()
                .noneMatch(a -> "ROLE_USER".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority())));
        assertEquals(JwtTokenType.ACCESS, access.type());
    }

    @Test
    void testRejectsGarbageToken() {
        // 2.Act / 3.Assert
        assertNull(service(SECRET, 15, 30).parseToken("not-a-jwt"));
    }

    @Test
    void testRejectsTokenSignedWithAnotherKey() {
        // 1.Arrange
        final MockHttpServletResponse response = issue(service(SECRET, 15, 30), principal());
        final JwtTokenService other = service("a-completely-different-secret-value-xx", 15, 30);

        // 2.Act / 3.Assert
        assertNull(other.parseToken(response.getHeader(JwtTokenService.ACCESS_TOKEN_HEADER)));
    }

    @Test
    void testRejectsExpiredToken() {
        // 1.Arrange — negative lifetime forces immediate expiry
        final JwtTokenService expired = service(SECRET, -1, -1);
        final MockHttpServletResponse response = issue(expired, principal());

        // 2.Act / 3.Assert
        assertNull(expired.parseToken(response.getHeader(JwtTokenService.ACCESS_TOKEN_HEADER)));
    }

    @Test
    void testAuthenticatedUserLinkIsHostRelativeApiUsersPath() {
        // 1.Arrange / 2.Act
        final MockHttpServletResponse response = issue(service(SECRET, 15, 30), principal());
        final String link = response.getHeader("Link");

        // 3.Assert — relative to context; no scheme/host baked in (S-05)
        assertNotNull(link);
        assertTrue(link.contains("</webapp/api/users/42>"));
        assertTrue(link.contains("rel=\"authenticated-user\""));
        assertFalse(link.contains("http://"));
        assertFalse(link.contains("https://"));
    }

    private static boolean hasPasswordResetOtp(final RydenUserDetails principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> RydenAuthorities.PASSWORD_RESET_OTP.equals(a.getAuthority()));
    }
}
