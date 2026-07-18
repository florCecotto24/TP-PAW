package ar.edu.itba.paw.webapp.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import ar.edu.itba.paw.webapp.config.properties.AppSecurityJwtProperties;
import ar.edu.itba.paw.webapp.security.auth.RydenAuthorities;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

@Service
public final class JwtTokenService implements TokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenService.class);

    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_PASSWORD_VERSION = "passwordVersion";

    /** Password-reset OTP grant: short access window; never renewed via refresh. */
    private static final int PWD_RESET_ACCESS_MAX_MINUTES = 5;

    static final String ACCESS_TOKEN_HEADER = "X-Access-Token";
    static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";
    static final String AUTHENTICATED_USER_REL = "authenticated-user";

    private final AppSecurityJwtProperties properties;
    private final SecretKey signingKey;

    public JwtTokenService(final AppSecurityJwtProperties properties, final Environment environment) {
        this.properties = properties;
        this.signingKey = resolveSigningKey(properties.secret(), environment);
    }

    @Override
    public void attachTokenHeaders(final HttpServletResponse response,
                                   final RydenUserDetails principal,
                                   final HttpServletRequest request) {
        final boolean passwordResetGrant = hasPasswordResetOtp(principal);
        final int accessMinutes = passwordResetGrant
                ? Math.min(properties.accessTokenMinutes(), PWD_RESET_ACCESS_MAX_MINUTES)
                : properties.accessTokenMinutes();
        response.setHeader(ACCESS_TOKEN_HEADER,
                generateToken(principal, JwtTokenType.ACCESS, accessMinutes, principal.getAuthorities()));
        // Password-reset OTP: never emit refresh. Refresh reloads roles from the DB and would
        // turn a one-time code into a full multi-day session.
        if (!passwordResetGrant) {
            response.setHeader(REFRESH_TOKEN_HEADER,
                    generateToken(principal, JwtTokenType.REFRESH, properties.refreshTokenDays() * 24 * 60,
                            principal.getAuthorities()));
        }
        final String userUri = buildUserUri(request, principal.getUserId());
        response.addHeader("Link", String.format("<%s>; rel=\"%s\"", userUri, AUTHENTICATED_USER_REL));
    }

    @Override
    public void attachAuthenticatedUserLink(final HttpServletResponse response,
                                            final RydenUserDetails principal,
                                            final HttpServletRequest request) {
        final String userUri = buildUserUri(request, principal.getUserId());
        response.addHeader("Link", String.format("<%s>; rel=\"%s\"", userUri, AUTHENTICATED_USER_REL));
    }

    @Override
    public ParsedJwt parseToken(final String token) {
        try {
            final Jws<Claims> parsed = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token);
            final Claims claims = parsed.getPayload();
            final JwtTokenType type = JwtTokenType.fromClaim(String.valueOf(claims.get(CLAIM_TOKEN_TYPE)));
            return new ParsedJwt(type, toPrincipal(claims));
        } catch (ExpiredJwtException ex) {
            LOGGER.atDebug().setCause(ex).log("Expired JWT token");
        } catch (SignatureException ex) {
            LOGGER.atDebug().setCause(ex).log("Invalid JWT signature");
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException ex) {
            LOGGER.atDebug().setCause(ex).log("Invalid JWT token");
        } catch (JwtException ex) {
            // e.g. WeakKeyException when a stored token's alg (HS512) does not fit the HMAC key size
            LOGGER.atDebug().setCause(ex).log("Rejected JWT token");
        }
        return null;
    }

    private String generateToken(final RydenUserDetails principal,
                                 final JwtTokenType type,
                                 final int lifetimeMinutes,
                                 final Collection<? extends GrantedAuthority> authorities) {
        final Instant now = Instant.now();
        final Instant expiry = now.plusSeconds((long) lifetimeMinutes * 60L);

        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(principal.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim(CLAIM_TOKEN_TYPE, type.getClaimValue())
                .claim(CLAIM_USER_ID, principal.getUserId())
                .claim(CLAIM_PASSWORD_VERSION, principal.getPasswordVersion())
                .claim("authorities", encodeAuthorities(authorities))
                // SHA-256-derived key is 256 bits → HS256 (HS512 would throw WeakKeyException).
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    private RydenUserDetails toPrincipal(final Claims claims) {
        final Collection<? extends GrantedAuthority> authorities =
                JwtAuthorityCodec.decode(claims.getSubject(), claims.get("authorities", String.class));
        // Display names and roleAssignedBy stay out of the JWT (PII / stale metadata); load via API.
        // Missing passwordVersion (pre-migration tokens) → 0, matching the DB default.
        final Integer passwordVersionClaim = claims.get(CLAIM_PASSWORD_VERSION, Integer.class);
        final int passwordVersion = passwordVersionClaim != null ? passwordVersionClaim : 0;
        return new RydenUserDetails(
                claims.get(CLAIM_USER_ID, Long.class),
                claims.getSubject(),
                null,
                null,
                null,
                authorities,
                null,
                passwordVersion);
    }

    private static boolean hasPasswordResetOtp(final RydenUserDetails principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> RydenAuthorities.PASSWORD_RESET_OTP.equals(a.getAuthority()));
    }

    private static String encodeAuthorities(final Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }

    /**
     * Relative user URN for {@code Link: rel=authenticated-user}. Host-relative so reverse proxies
     * / Vite do not bake a wrong absolute origin; clients resolve against the current API base.
     */
    private static String buildUserUri(final HttpServletRequest request, final long userId) {
        final String context = request.getContextPath() == null ? "" : request.getContextPath();
        // Host-relative path via UriBuilder (same policy as RestUriUtils): avoid baking a wrong origin.
        return UriBuilder.fromPath(context)
                .path("api")
                .path("users")
                .path(Long.toString(userId))
                .build()
                .toString();
    }

    private static SecretKey resolveSigningKey(final String configuredSecret, final Environment environment) {
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            return Keys.hmacShaKeyFor(sha256(configuredSecret));
        }
        if (environment != null && environment.acceptsProfiles(Profiles.of("local", "test"))) {
            LOGGER.atWarn().log("app.security.jwt.secret is blank — using a random key (tokens reset on restart)");
            return Jwts.SIG.HS256.key().build();
        }
        throw new IllegalStateException("app.security.jwt.secret must be configured outside the local/test "
                + "profiles; refusing to start with a random, ephemeral JWT signing key.");
    }

    private static byte[] sha256(final String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must be available to derive the JWT signing key", e);
        }
    }
}
