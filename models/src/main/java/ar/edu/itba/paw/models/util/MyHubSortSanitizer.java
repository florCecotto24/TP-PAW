package ar.edu.itba.paw.models.util;

import java.util.Set;

/**
 * Whitelist for combined sort keys ({@code field,direction}) used on owner hub and “my reservations” list UIs.
 */
public final class MyHubSortSanitizer {

    private static final Set<String> VALID =
            Set.of("date,desc", "date,asc", "price,asc", "price,desc", "rating,asc", "rating,desc");

    private MyHubSortSanitizer() {
    }

    /**
     * @param sort       raw query value (may be null or blank)
     * @param defaultSort returned when {@code sort} is null, blank, or not in the whitelist
     */
    public static String sanitize(final String sort, final String defaultSort) {
        if (sort == null || sort.isBlank()) {
            return defaultSort;
        }
        return VALID.contains(sort) ? sort : defaultSort;
    }
}
