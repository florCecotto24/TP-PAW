package ar.edu.itba.paw.webapp.support;

import java.util.Locale;

import ar.edu.itba.paw.models.util.search.MyHubSortSanitizer;

/** Maps REST {@code sort} query tokens to internal {@code field,direction} keys. */
public final class RestCarSortMapper {

    private static final String DEFAULT_SORT = "date,desc";

    private RestCarSortMapper() {
    }

    public static String toInternalSort(final String restSort) {
        if (restSort == null || restSort.isBlank()) {
            return DEFAULT_SORT;
        }
        return switch (restSort.trim().toLowerCase(Locale.ROOT)) {
            case "price_asc" -> "price,asc";
            case "price_desc" -> "price,desc";
            case "rating_desc" -> "rating,desc";
            case "recent" -> "date,desc";
            case "name" -> "date,desc";
            default -> MyHubSortSanitizer.sanitize(restSort, DEFAULT_SORT);
        };
    }

    public static boolean isBrowseShortcut(final String restSort) {
        if (restSort == null || restSort.isBlank()) {
            return false;
        }
        final String normalized = restSort.trim().toLowerCase(Locale.ROOT);
        return "price_asc".equals(normalized) || "recent".equals(normalized);
    }
}
