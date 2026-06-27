package ar.edu.itba.paw.webapp.security.jwt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * Resolves authentication from {@code Authorization: Basic} (login) or {@code Bearer} (access/refresh).
 * Attaches token headers on successful Basic auth or refresh (LINEAMIENTOS §1.8).
 */
public final class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BASIC_PREFIX = "Basic ";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public JwtAuthenticationFilter(final AuthenticationManager authenticationManager,
                                   final TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (header.startsWith(BASIC_PREFIX)) {
            handleBasic(request, response, header.substring(BASIC_PREFIX.length()).trim());
        } else if (header.startsWith(BEARER_PREFIX)) {
            handleBearer(request, response, header.substring(BEARER_PREFIX.length()).trim());
        }

        filterChain.doFilter(request, response);
    }

    private void handleBasic(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final String credentialsBase64) {
        try {
            final byte[] decoded = Base64.decode(credentialsBase64.getBytes(StandardCharsets.UTF_8));
            final String credentials = new String(decoded, StandardCharsets.UTF_8);
            final int colon = credentials.indexOf(':');
            if (colon < 0) {
                return;
            }
            final String email = credentials.substring(0, colon);
            final String password = credentials.substring(colon + 1);
            final Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));
            SecurityContextHolder.getContext().setAuthentication(auth);
            if (auth.getPrincipal() instanceof RydenUserDetails principal) {
                tokenService.attachTokenHeaders(response, principal, request);
            }
        } catch (RuntimeException ignored) {
            // Invalid credentials: leave context anonymous; entry point returns 401 on protected routes.
        }
    }

    private void handleBearer(final HttpServletRequest request,
                              final HttpServletResponse response,
                              final String token) {
        final TokenService.ParsedJwt parsed = tokenService.parseToken(token);
        if (parsed == null) {
            return;
        }

        if (parsed.type().isRefresh()) {
            tokenService.attachTokenHeaders(response, parsed.principal(), request);
            return;
        }

        final UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(parsed.principal(), null, parsed.principal().getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
