package ar.edu.itba.paw.models.util;

import java.util.Locale;

public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(final String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
