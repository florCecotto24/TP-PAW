package ar.edu.itba.paw.services;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ar.edu.itba.paw.models.Neighborhood;

/**
 * Rough matching of search text tokens with neighborhood names (Levenshtein bounded).
 */
public final class NeighborhoodNameMatcher {

    private NeighborhoodNameMatcher() {
    }

    /**
     * For each token of {@code query} with length ≥ {@code minTokenLength}, if the Levenshtein distance
     * to the name of a neighborhood (comparison in lowercase) is ≤ {@code maxDistance}, the id is included.
     */
    public static List<Long> idsMatchingFuzzyTokens(
            final String query,
            final List<Neighborhood> neighborhoods,
            final int maxDistance,
            final int minTokenLength) {
        final Set<Long> ids = new LinkedHashSet<>();
        if (query == null || query.isBlank() || neighborhoods == null || neighborhoods.isEmpty()) {
            return List.copyOf(ids);
        }
        final String[] rawTokens = query.trim().toLowerCase(Locale.ROOT).split("\\s+");
        final List<String> tokens = new ArrayList<>();
        for (final String t : rawTokens) {
            if (t.length() >= minTokenLength) {
                tokens.add(t);
            }
        }
        if (tokens.isEmpty()) {
            return List.copyOf(ids);
        }
        for (final Neighborhood n : neighborhoods) {
            final String name = n.getName().toLowerCase(Locale.ROOT);
            for (final String token : tokens) {
                if (levenshtein(token, name) <= maxDistance) {
                    ids.add(n.getId());
                    break;
                }
            }
        }
        return List.copyOf(ids);
    }

    static int levenshtein(final String a, final String b) {
        if (a == null || b == null) {
            if (a == null && b == null) {
                return 0;
            }
            return a == null ? b.length() : a.length();
        }
        final int n = a.length();
        final int m = b.length();
        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            final char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                final int cost = ca == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(
                        Math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost);
            }
            final int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[m];
    }
}
