package ar.edu.itba.paw.models.util.media;

import java.util.Locale;

/**
 * Magic-byte sniffing for document uploads (KYC, receipts, insurance). Declared {@code Content-Type}
 * alone is not trusted: clients can spoof MIME while sending SVG or other markup.
 */
public final class BinaryMagicBytes {

    private BinaryMagicBytes() {
    }

    /**
     * Returns a canonical MIME when the leading bytes match a known raster image or PDF; otherwise
     * {@code null}.
     */
    public static String sniffMime(final byte[] data) {
        if (data == null || data.length < 2) {
            return null;
        }
        if (data.length >= 4
                && data[0] == 0x25
                && data[1] == 0x50
                && data[2] == 0x44
                && data[3] == 0x46) {
            return "application/pdf";
        }
        if (data[0] == (byte) 0xff && data[1] == (byte) 0xd8) {
            return "image/jpeg";
        }
        if (data.length >= 4
                && data[0] == (byte) 0x89
                && data[1] == 0x50
                && data[2] == 0x4e
                && data[3] == 0x47) {
            return "image/png";
        }
        if (data.length >= 3
                && data[0] == 0x47
                && data[1] == 0x49
                && data[2] == 0x46) {
            return "image/gif";
        }
        if (data.length >= 12
                && data[0] == 0x52
                && data[1] == 0x49
                && data[2] == 0x46
                && data[3] == 0x46
                && data[8] == 0x57
                && data[9] == 0x45
                && data[10] == 0x42
                && data[11] == 0x50) {
            return "image/webp";
        }
        return null;
    }

    /**
     * True when {@code declaredContentType} is on the document allowlist and the payload magic bytes
     * match that type.
     */
    public static boolean matchesDeclared(final String declaredContentType, final byte[] data) {
        final String declared = normalize(declaredContentType);
        if (declared == null) {
            return false;
        }
        final String sniffed = sniffMime(data);
        return sniffed != null && sniffed.equals(declared);
    }

    private static String normalize(final String contentType) {
        if (contentType == null) {
            return null;
        }
        final String t = contentType.trim().toLowerCase(Locale.ROOT);
        final int semi = t.indexOf(';');
        return semi < 0 ? t : t.substring(0, semi).trim();
    }
}
