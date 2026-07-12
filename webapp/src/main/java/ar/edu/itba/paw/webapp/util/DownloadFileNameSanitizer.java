package ar.edu.itba.paw.webapp.util;

import ar.edu.itba.paw.models.util.format.TextTruncationLimits;

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
        return trimmed.length() > TextTruncationLimits.DOWNLOAD_FILENAME
                ? trimmed.substring(0, TextTruncationLimits.DOWNLOAD_FILENAME)
                : trimmed;
    }
}
