package ar.edu.itba.paw.webapp.support.converter;

import java.util.Locale;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import ar.edu.itba.paw.models.util.rules.SupportedLocales;

/**
 * Binds the {@code lang} query parameter of the locale-toggle endpoint into a {@link Locale} using
 * {@link SupportedLocales#parse(String)}. Unknown / unsupported tags map to {@code null} so the
 * controller can keep its "silently ignore unknown languages" contract instead of returning a 400.
 * Wired in {@link ar.edu.itba.paw.webapp.config.WebConfig#addFormatters}.
 */
public final class StringToSupportedLocaleConverter implements Converter<String, Locale> {

    @Override
    @Nullable
    public Locale convert(@NonNull final String source) {
        return SupportedLocales.parse(source).orElse(null);
    }
}
