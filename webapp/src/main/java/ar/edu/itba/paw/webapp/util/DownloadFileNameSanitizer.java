package ar.edu.itba.paw.webapp.util;

/** Safe {@code Content-Disposition} filename for binary downloads. */
public final class DownloadFileNameSanitizer {

    private DownloadFileNameSanitizer() {
    }

    public static String sanitize(final String raw, final String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        final String trimmed = raw.trim().replace("\"", "'").replaceAll("[\\\\/:*?|<>\\r\\n]+", "_");
        if (trimmed.isBlank()) {
            return fallback;
        }
        return trimmed.length() > 120 ? trimmed.substring(0, 120) : trimmed;
    }
}
