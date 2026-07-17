package ar.edu.itba.paw.models.util.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Digests short-lived OTP codes for at-rest storage. The plaintext code is mailed to the user;
 * only the SHA-256 hex digest is persisted so a DB leak does not expose usable codes.
 */
public final class OtpCodeDigest {

    private OtpCodeDigest() {
    }

    /**
     * @param code plaintext OTP (trimmed); blank or null yields an empty digest string that will not match
     * @return lowercase hex SHA-256 of the UTF-8 bytes of {@code code.trim()}, or {@code ""} if blank
     */
    public static String sha256Hex(final String code) {
        if (code == null) {
            return "";
        }
        final String trimmed = code.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(trimmed.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must be available to digest OTP codes", e);
        }
    }
}
