package ar.edu.itba.paw.webapp.security.jwt;

/**
 * JWT claim discriminator for access vs refresh tokens (LINEAMIENTOS §1.8).
 */
public enum JwtTokenType {
    ACCESS("access"),
    REFRESH("refresh");

    private final String claimValue;

    JwtTokenType(final String claimValue) {
        this.claimValue = claimValue;
    }

    public String getClaimValue() {
        return claimValue;
    }

    public boolean isRefresh() {
        return this == REFRESH;
    }

    public static JwtTokenType fromClaim(final String value) {
        if (REFRESH.claimValue.equals(value)) {
            return REFRESH;
        }
        return ACCESS;
    }
}
