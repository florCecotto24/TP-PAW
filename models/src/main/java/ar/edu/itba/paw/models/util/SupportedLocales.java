package ar.edu.itba.paw.models.util;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The locales the UI accepts. Centralised so the resolver, the language toggle endpoint, and
 * {@code UserService#findUserPreferredLocale} agree on what's allowed. Default is Spanish — the
 * application is biased towards Argentine users.
 */
public final class SupportedLocales {

    public static final Locale SPANISH = Locale.forLanguageTag("es");
    public static final Locale ENGLISH = Locale.ENGLISH;
    public static final Locale DEFAULT = SPANISH;
    public static final Set<Locale> ALL = Set.of(SPANISH, ENGLISH);

    private SupportedLocales() {}

    /**
     * Resolves a BCP 47 (or legacy underscored) tag into a supported {@link Locale}. Only the language
     * subtag is inspected; region/variant subtags are intentionally collapsed (e.g. {@code es-AR} → {@code es}).
     */
    public static Optional<Locale> parse(final String tag) {
        if (tag == null || tag.isBlank()) {
            return Optional.empty();
        }
        final Locale candidate = Locale.forLanguageTag(tag.trim().replace('_', '-'));
        final String lang = candidate.getLanguage();
        if (lang == null || lang.isEmpty()) {
            return Optional.empty();
        }
        if ("es".equalsIgnoreCase(lang)) {
            return Optional.of(SPANISH);
        }
        if ("en".equalsIgnoreCase(lang)) {
            return Optional.of(ENGLISH);
        }
        return Optional.empty();
    }
}
