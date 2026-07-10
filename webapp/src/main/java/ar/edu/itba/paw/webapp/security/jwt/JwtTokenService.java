package ar.edu.itba.paw.webapp.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import ar.edu.itba.paw.webapp.config.properties.AppSecurityJwtProperties;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

@Service
public final class JwtTokenService implements TokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenService.class);

    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_FORENAME = "forename";
    private static final String CLAIM_SURNAME = "surname";
    private static final String CLAIM_ROLE_ASSIGNED_BY = "roleAssignedBy";

    static final String ACCESS_TOKEN_HEADER = "X-Access-Token";
    static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";
    static final String AUTHENTICATED_USER_REL = "authenticated-user";

    private final AppSecurityJwtProperties properties;
    private final SecretKey signingKey;

    public JwtTokenService(final AppSecurityJwtProperties properties) {
        this.properties = properties;
        this.signingKey = resolveSigningKey(properties.secret());
    }

    @Override
    public void attachTokenHeaders(final HttpServletResponse response,
                                   final RydenUserDetails principal,
                                   final HttpServletRequest request) {
        response.setHeader(ACCESS_TOKEN_HEADER, generateToken(principal, JwtTokenType.ACCESS));
        response.setHeader(REFRESH_TOKEN_HEADER, generateToken(principal, JwtTokenType.REFRESH));
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
        }
        return null;
    }

    private String generateToken(final RydenUserDetails principal, final JwtTokenType type) {
        final Instant now = Instant.now();
        final Instant expiry = type.isRefresh()
                ? now.plusSeconds((long) properties.refreshTokenDays() * 24 * 60 * 60)
                : now.plusSeconds((long) properties.accessTokenMinutes() * 60);

        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(principal.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim(CLAIM_TOKEN_TYPE, type.getClaimValue())
                .claim(CLAIM_USER_ID, principal.getUserId())
                .claim(CLAIM_FORENAME, principal.getForename())
                .claim(CLAIM_SURNAME, principal.getSurname())
                .claim(CLAIM_ROLE_ASSIGNED_BY, principal.getRoleAssignedBy().orElse(null))
                .claim("authorities", JwtAuthorityCodec.encode(principal))
                .signWith(signingKey)
                .compact();
    }

    private RydenUserDetails toPrincipal(final Claims claims) {
        final Long roleAssignedBy = claims.get(CLAIM_ROLE_ASSIGNED_BY, Long.class);
        final Collection<? extends GrantedAuthority> authorities =
                JwtAuthorityCodec.decode(claims.getSubject(), claims.get("authorities", String.class));
        return new RydenUserDetails(
                claims.get(CLAIM_USER_ID, Long.class),
                claims.getSubject(),
                claims.get(CLAIM_FORENAME, String.class),
                claims.get(CLAIM_SURNAME, String.class),
                null,
                authorities,
                roleAssignedBy);
    }

    private static String buildUserUri(final HttpServletRequest request, final long userId) {
        final StringBuilder uri = new StringBuilder();
        uri.append(request.getScheme()).append("://").append(request.getServerName());
        final int port = request.getServerPort();
        if (port != 80 && port != 443) {
            uri.append(':').append(port);
        }
        uri.append(request.getContextPath());
        if (!request.getContextPath().endsWith("/")) {
            uri.append('/');
        }
        uri.append("users/").append(userId);
        return uri.toString();
    }

    private static SecretKey resolveSigningKey(final String configuredSecret) {
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            return Keys.hmacShaKeyFor(configuredSecret.getBytes(StandardCharsets.UTF_8));
        }
        final byte[] random = new byte[64];
        new SecureRandom().nextBytes(random);
        LOGGER.atWarn().log("app.security.jwt.secret is blank — using a random key (tokens reset on restart)");
        return Keys.hmacShaKeyFor(Base64.getEncoder().encodeToString(random).getBytes(StandardCharsets.UTF_8));
    }
}
