package ar.edu.itba.paw.webapp.security.jwt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.api.common.VndMediaType;
import ar.edu.itba.paw.webapp.security.auth.RydenAuthorities;
import ar.edu.itba.paw.webapp.security.auth.exception.EmailNotValidatedException;
import ar.edu.itba.paw.webapp.security.auth.exception.LegacyPasswordMailedException;
import ar.edu.itba.paw.webapp.security.auth.exception.OtpRateLimitedAuthenticationException;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.security.auth.userdetails.UserRoleAuthorities;

/**
 * Resolves authentication from {@code Authorization: Basic} (login) or {@code Bearer} (access/refresh).
 * A refresh token authenticates the request and rotates the token pair into response headers.
 * Refresh always reloads the user and roles from the database so revoked accounts and role changes
 * take effect on the next rotation (JWT claims alone are not authoritative for refresh).
 */
public final class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BASIC_PREFIX = "Basic ";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final ObjectMapper ERROR_MAPPER = new ObjectMapper();

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserService userService;

    public JwtAuthenticationFilter(final AuthenticationManager authenticationManager,
                                   final TokenService tokenService,
                                   final UserService userService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        try {
            if (StringUtils.hasText(header)) {
                if (header.startsWith(BASIC_PREFIX)) {
                    if (!handleBasic(request, response, header.substring(BASIC_PREFIX.length()).trim())) {
                        return;
                    }
                } else if (header.startsWith(BEARER_PREFIX)) {
                    handleBearer(request, response, header.substring(BEARER_PREFIX.length()).trim());
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** @return {@code true} to continue the chain; {@code false} if an error body was already written. */
    private boolean handleBasic(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final String credentialsBase64) throws IOException {
        final String credentials;
        try {
            final byte[] decoded = Base64.decode(credentialsBase64.getBytes(StandardCharsets.UTF_8));
            credentials = new String(decoded, StandardCharsets.UTF_8);
        } catch (final IllegalArgumentException ex) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid_credentials",
                    "Malformed Basic credentials.");
            return false;
        }
        final int colon = credentials.indexOf(':');
        if (colon < 0) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid_credentials",
                    "Malformed Basic credentials.");
            return false;
        }
        final String email = credentials.substring(0, colon);
        final String password = credentials.substring(colon + 1);
        try {
            final Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));
            applyAuthentication(auth, request);
            if (auth.getPrincipal() instanceof RydenUserDetails principal) {
                tokenService.attachTokenHeaders(response, principal, request);
                // OTP Basic probe may hit GET / (permitAll) to discover authenticated-user.
                // Do not leave a password-reset principal authenticated for business routes.
                if (hasPasswordResetOtp(principal) && !isUserPasswordPatch(request)) {
                    SecurityContextHolder.clearContext();
                }
            }
            return true;
        } catch (final EmailNotValidatedException ex) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "email_not_validated",
                    "Email address is not verified.");
            return false;
        } catch (final LegacyPasswordMailedException ex) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "legacy_password_mailed",
                    "A new password was emailed to this legacy account.");
            return false;
        } catch (final OtpRateLimitedAuthenticationException ex) {
            writeError(response, 429, "otp_rate_limited",
                    "Too many failed one-time code attempts. Try again later.");
            return false;
        } catch (final AuthenticationException ex) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid_credentials",
                    "Invalid email or password.");
            return false;
        }
    }

    private void handleBearer(final HttpServletRequest request,
                              final HttpServletResponse response,
                              final String token) {
        final TokenService.ParsedJwt parsed = tokenService.parseToken(token);
        if (parsed == null) {
            return;
        }

        final RydenUserDetails principal;
        if (parsed.type().isRefresh()) {
            final Optional<RydenUserDetails> refreshed = reloadPrincipalFromDatabase(parsed.principal().getUserId());
            if (refreshed.isEmpty()) {
                return;
            }
            principal = refreshed.get();
        } else {
            principal = parsed.principal();
            // Access tokens issued for password-reset OTP must only authorize PATCH /users/{id}.
            if (hasPasswordResetOtp(principal) && !isUserPasswordPatch(request)) {
                return;
            }
        }

        final UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        applyAuthentication(authentication, request);

        if (parsed.type().isRefresh()) {
            tokenService.attachTokenHeaders(response, principal, request);
        }
    }

    private static boolean hasPasswordResetOtp(final RydenUserDetails principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> RydenAuthorities.PASSWORD_RESET_OTP.equals(a.getAuthority()));
    }

    /** {@code PATCH …/api/users/{id}} — the only business operation allowed under OTP grant. */
    private static boolean isUserPasswordPatch(final HttpServletRequest request) {
        if (!"PATCH".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        final String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        // Context path may be /webapp or /paw-2026a-08; match …/api/users/{numericId}
        return uri.matches(".*/api/users/\\d+/?$");
    }

    /**
     * Rebuilds the security principal from current DB state. Missing users do not authenticate
     * and do not rotate tokens. Blocked users remain eligible (same policy as login).
     */
    private Optional<RydenUserDetails> reloadPrincipalFromDatabase(final long userId) {
        final Optional<User> userOpt = userService.getUserById(userId);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        final User user = userOpt.get();
        return Optional.of(new RydenUserDetails(
                user.getId(),
                user.getEmail(),
                user.getForename(),
                user.getSurname(),
                null,
                new ArrayList<>(UserRoleAuthorities.fromUserRoles(userService.findRolesForUser(user.getId()))),
                user.getRoleAssignedBy().orElse(null)));
    }

    private static void applyAuthentication(final Authentication authentication,
                                            final HttpServletRequest request) {
        final SecurityContext context = SecurityContextHolder.createEmptyContext();
        if (authentication instanceof UsernamePasswordAuthenticationToken usernamePassword
                && usernamePassword.getDetails() == null) {
            usernamePassword.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        }
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private static void writeError(final HttpServletResponse response, final int status,
                                   final String code, final String message) throws IOException {
        response.setStatus(status);
        if (status == HttpServletResponse.SC_UNAUTHORIZED) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"ryden\"");
        }
        response.setContentType(VndMediaType.ERROR_V1_JSON);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("code", code);
        body.put("message", message);
        response.getWriter().write(ERROR_MAPPER.writeValueAsString(body));
    }
}
