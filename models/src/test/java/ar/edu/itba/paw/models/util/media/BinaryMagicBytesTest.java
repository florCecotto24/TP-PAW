package ar.edu.itba.paw.models.util.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BinaryMagicBytesTest {

    @Test
    void testSniffMimeRecognizesPdfJpegPng() {
        // 1.Arrange / 2.Act / 3.Assert
        assertEquals("application/pdf", BinaryMagicBytes.sniffMime(new byte[] {0x25, 0x50, 0x44, 0x46}));
        assertEquals("image/jpeg", BinaryMagicBytes.sniffMime(new byte[] {(byte) 0xff, (byte) 0xd8, 0x00}));
        assertEquals(
                "image/png",
                BinaryMagicBytes.sniffMime(new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}));
    }

    @Test
    void testSniffMimeRejectsUnknownAndSvgMarkup() {
        // 1.Arrange / 2.Act / 3.Assert
        assertNull(BinaryMagicBytes.sniffMime(null));
        assertNull(BinaryMagicBytes.sniffMime(new byte[] {0x3c, 0x73, 0x76, 0x67})); // "<svg"
        assertNull(BinaryMagicBytes.sniffMime(new byte[] {0x00}));
    }

    @Test
    void testMatchesDeclaredRequiresAllowlistAndMagicAgreement() {
        // 1.Arrange
        final byte[] jpeg = new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00};
        final byte[] pdf = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2d};

        // 2.Act / 3.Assert
        assertTrue(BinaryMagicBytes.matchesDeclared("image/jpeg", jpeg));
        assertTrue(BinaryMagicBytes.matchesDeclared("application/pdf; charset=binary", pdf));
        assertFalse(BinaryMagicBytes.matchesDeclared("image/png", jpeg));
        assertFalse(BinaryMagicBytes.matchesDeclared("image/svg+xml", jpeg));
        assertFalse(BinaryMagicBytes.matchesDeclared("image/jpeg", pdf));
    }
}
