package ar.edu.itba.paw.models.util.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OtpCodeDigestTest {

    @Test
    void testSha256HexIsStable64LowercaseHex() {
        // 1.Arrange / 2.Act
        final String digest = OtpCodeDigest.sha256Hex("123456");

        // 3.Assert
        Assertions.assertEquals(64, digest.length());
        Assertions.assertTrue(digest.matches("[0-9a-f]{64}"));
        Assertions.assertEquals(digest, OtpCodeDigest.sha256Hex(" 123456 "));
        Assertions.assertNotEquals(digest, OtpCodeDigest.sha256Hex("654321"));
    }

    @Test
    void testSha256HexBlankYieldsEmpty() {
        Assertions.assertEquals("", OtpCodeDigest.sha256Hex(null));
        Assertions.assertEquals("", OtpCodeDigest.sha256Hex("   "));
    }
}
