package ar.edu.itba.paw.webapp.security.jwt;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * Issues and validates stateless JWT access/refresh tokens (LINEAMIENTOS §1.8).
 */
public interface TokenService {

    void attachTokenHeaders(HttpServletResponse response, RydenUserDetails principal, HttpServletRequest request);

    /**
     * Publishes {@code Link: rel=authenticated-user} without issuing JWT credentials.
     * Used for password-reset OTP Basic probes that must discover the user URN only.
     */
    void attachAuthenticatedUserLink(HttpServletResponse response, RydenUserDetails principal, HttpServletRequest request);

    ParsedJwt parseToken(String token);

    record ParsedJwt(JwtTokenType type, RydenUserDetails principal) {
    }
}
