package ar.edu.itba.paw.webapp.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.FilterChain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.security.UserRole;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.config.properties.AppSecurityJwtProperties;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String SECRET = "unit-test-secret-value-please-change-me-long-enough";

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    private JwtTokenService tokenService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        final AppSecurityJwtProperties props = new AppSecurityJwtProperties(SECRET, "ryden", 15, 30);
        final MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        tokenService = new JwtTokenService(props, environment);
        filter = new JwtAuthenticationFilter(authenticationManager, tokenService, userService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void testRefreshRejectedWhenPasswordVersionMismatch() throws Exception {
        // 1.Arrange — token issued at version 0; DB now at 1 after password change
        final String refresh = issueRefresh(0);
        Mockito.when(userService.getUserById(42L)).thenReturn(Optional.of(dbUser(true, 1)));

        final MockHttpServletRequest request = bearerRequest(refresh);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicBoolean authenticatedDuringChain = new AtomicBoolean(false);

        // 2.Act
        filter.doFilter(request, response, capturingChain(authenticatedDuringChain));

        // 3.Assert — no rotation; request continues unauthenticated
        assertNull(response.getHeader(JwtTokenService.ACCESS_TOKEN_HEADER));
        assertNull(response.getHeader(JwtTokenService.REFRESH_TOKEN_HEADER));
        assertFalse(authenticatedDuringChain.get());
    }

    @Test
    void testRefreshRejectedWhenEmailNotValidated() throws Exception {
        // 1.Arrange
        final String refresh = issueRefresh(0);
        Mockito.when(userService.getUserById(42L)).thenReturn(Optional.of(dbUser(false, 0)));

        final MockHttpServletRequest request = bearerRequest(refresh);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicBoolean authenticatedDuringChain = new AtomicBoolean(false);

        // 2.Act
        filter.doFilter(request, response, capturingChain(authenticatedDuringChain));

        // 3.Assert
        assertNull(response.getHeader(JwtTokenService.ACCESS_TOKEN_HEADER));
        assertFalse(authenticatedDuringChain.get());
    }

    @Test
    void testRefreshRotatesWhenCredentialsEpochMatches() throws Exception {
        // 1.Arrange
        final String refresh = issueRefresh(2);
        Mockito.when(userService.getUserById(42L)).thenReturn(Optional.of(dbUser(true, 2)));
        Mockito.when(userService.findRolesForUser(42L)).thenReturn(List.of(UserRole.USER));

        final MockHttpServletRequest request = bearerRequest(refresh);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicBoolean authenticatedDuringChain = new AtomicBoolean(false);

        // 2.Act
        filter.doFilter(request, response, capturingChain(authenticatedDuringChain));

        // 3.Assert
        assertTrue(authenticatedDuringChain.get());
        assertNotNull(response.getHeader(JwtTokenService.ACCESS_TOKEN_HEADER));
        assertNotNull(response.getHeader(JwtTokenService.REFRESH_TOKEN_HEADER));
        final TokenService.ParsedJwt rotated =
                tokenService.parseToken(response.getHeader(JwtTokenService.REFRESH_TOKEN_HEADER));
        assertNotNull(rotated);
        assertEquals(2, rotated.principal().getPasswordVersion());
    }

    @Test
    void testAccessRejectedWhenPasswordVersionMismatch() throws Exception {
        // 1.Arrange
        final String access = issueAccess(0);
        Mockito.when(userService.getUserById(42L)).thenReturn(Optional.of(dbUser(true, 1)));

        final MockHttpServletRequest request = bearerRequest(access);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicBoolean authenticatedDuringChain = new AtomicBoolean(false);

        // 2.Act
        filter.doFilter(request, response, capturingChain(authenticatedDuringChain));

        // 3.Assert
        assertFalse(authenticatedDuringChain.get());
    }

    @Test
    void testAccessAcceptedWhenCredentialsEpochMatches() throws Exception {
        // 1.Arrange
        final String access = issueAccess(4);
        Mockito.when(userService.getUserById(42L)).thenReturn(Optional.of(dbUser(true, 4)));

        final MockHttpServletRequest request = bearerRequest(access);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicBoolean authenticatedDuringChain = new AtomicBoolean(false);

        // 2.Act
        filter.doFilter(request, response, capturingChain(authenticatedDuringChain));

        // 3.Assert
        assertTrue(authenticatedDuringChain.get());
    }

    private static FilterChain capturingChain(final AtomicBoolean authenticatedDuringChain) {
        return (req, res) -> authenticatedDuringChain.set(
                SecurityContextHolder.getContext().getAuthentication() != null
                        && SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
    }

    private static User dbUser(final boolean emailValidated, final int passwordVersion) {
        return User.builder()
                .id(42L)
                .email("ana@example.com")
                .forename("Ana")
                .surname("Beltran")
                .emailValidated(emailValidated)
                .passwordVersion(passwordVersion)
                .userRole(UserRole.USER)
                .memberSince(java.time.LocalDate.of(2024, 1, 1))
                .build();
    }

    private String issueRefresh(final int passwordVersion) {
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/webapp");
        tokenService.attachTokenHeaders(response, principal(passwordVersion), request);
        return response.getHeader(JwtTokenService.REFRESH_TOKEN_HEADER);
    }

    private String issueAccess(final int passwordVersion) {
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/webapp");
        tokenService.attachTokenHeaders(response, principal(passwordVersion), request);
        return response.getHeader(JwtTokenService.ACCESS_TOKEN_HEADER);
    }

    private static RydenUserDetails principal(final int passwordVersion) {
        final Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_USER"));
        return RydenUserDetails.builder()
                .userId(42L)
                .email("ana@example.com")
                .forename("Ana")
                .surname("Beltran")
                .encodedPassword("hash")
                .authorities(authorities)
                .passwordVersion(passwordVersion)
                .build();
    }

    private static MockHttpServletRequest bearerRequest(final String token) {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/webapp/api/cars");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
