package ar.edu.itba.paw.models.util;

import java.util.Locale;
import java.util.Set;

/**
 * Lowercase reservation status tokens allowed in hub search / filter query params and criteria building.
 */
public final class ReservationHubStatusWhitelist {

    private static final Set<String> STATUSES =
            Set.of("pending", "accepted", "started", "cancelled",
                    "cancelled_by_rider", "cancelled_by_owner", "cancelled_due_to_missing_payment_proof",
                    "finished");

    private ReservationHubStatusWhitelist() {
    }

    /** Whether {@code normalizedLowercase} is an allowed status token (already trimmed and lowercased). */
    public static boolean contains(final String normalizedLowercase) {
        return STATUSES.contains(normalizedLowercase);
    }

    /**
     * Normalizes a single client-supplied status token; {@code null} when absent, blank, or not whitelisted.
     */
    public static String normalizeStatusQueryParam(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        final String t = raw.trim().toLowerCase(Locale.ROOT);
        return STATUSES.contains(t) ? t : null;
    }
}
