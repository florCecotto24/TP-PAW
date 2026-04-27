package ar.edu.itba.paw.services.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.common.util.Levenshtein;

/**
 * Aproximated matching of search text tokens with neighborhood names (Levenshtein bounded).
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
                if (Levenshtein.distance(token, name) <= maxDistance) {
                    ids.add(n.getId());
                    break;
                }
            }
        }
        return List.copyOf(ids);
    }
}
