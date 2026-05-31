package ar.edu.itba.paw.webapp.support.converter;

import ar.edu.itba.paw.webapp.support.ReservationViewerRole;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Case-insensitive {@code String → ReservationViewerRole} converter so {@code @RequestParam} can
 * bind the legacy lowercase tokens ({@code owner} / {@code rider}) directly without controllers
 * needing whitelist {@code if} checks. Unknown values return {@code null}, which lets handlers using
 * {@code @RequestParam(defaultValue = ...)} pick a sane fallback or use Spring's standard
 * {@code typeMismatch} flow when no default is configured. Wired in
 * {@link ar.edu.itba.paw.webapp.config.WebConfig#addFormatters} (not a {@code @Component}: the
 * converter is stateless and only relevant to MVC binding).
 */
public final class StringToReservationViewerRoleConverter implements Converter<String, ReservationViewerRole> {

    @Override
    @Nullable
    public ReservationViewerRole convert(@NonNull final String source) {
        return ReservationViewerRole.fromLegacyString(source).orElse(null);
    }
}
