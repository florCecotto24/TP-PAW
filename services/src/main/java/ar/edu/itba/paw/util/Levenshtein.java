package ar.edu.itba.paw.util;

/**
 * Levenshtein edit distance between two strings (cost of one insertion, deletion or substitution).
 */
public final class Levenshtein {

    private Levenshtein() {
    }

    /**
     * @return distance between {@code a} and {@code b}; if one is {@code null} and the other is not, the distance is the
     *         length of the non-null; if both are {@code null}, {@code 0}.
     */
    public static int distance(final String a, final String b) {
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
