package ar.edu.itba.paw.webapp.config.properties;

import org.springframework.core.env.Environment;

/**
 * Bound view of {@code app.security.jwt.*} for stateless access/refresh tokens.
 */
public record AppSecurityJwtProperties(
        String secret,
        String issuer,
        int accessTokenMinutes,
        int refreshTokenDays) {

    private static final int DEFAULT_ACCESS_TOKEN_MINUTES = 15;
    private static final int DEFAULT_REFRESH_TOKEN_DAYS = 30;
    private static final String DEFAULT_ISSUER = "ryden";

    public static AppSecurityJwtProperties fromEnvironment(final Environment environment) {
        return new AppSecurityJwtProperties(
                environment.getProperty("app.security.jwt.secret", ""),
                environment.getProperty("app.security.jwt.issuer", DEFAULT_ISSUER),
                environment.getProperty("app.security.jwt.access-token-minutes", Integer.class, DEFAULT_ACCESS_TOKEN_MINUTES),
                environment.getProperty("app.security.jwt.refresh-token-days", Integer.class, DEFAULT_REFRESH_TOKEN_DAYS));
    }
}
