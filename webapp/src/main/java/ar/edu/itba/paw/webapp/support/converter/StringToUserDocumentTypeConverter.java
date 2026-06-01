package ar.edu.itba.paw.webapp.support.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import ar.edu.itba.paw.models.domain.UserDocumentType;

/**
 * Case-insensitive {@code String → UserDocumentType} converter. JSPs always emit the canonical
 * uppercase token ({@code LICENSE} / {@code IDENTITY}), but URL-driven downloads ({@code /profile/documents/{documentType}})
 * may receive any casing. Returning {@code null} for unknown values lets controllers respond with
 * 404 / redirect without dealing with {@link IllegalArgumentException}. Wired in
 * {@link ar.edu.itba.paw.webapp.config.WebConfig#addFormatters}.
 */
public final class StringToUserDocumentTypeConverter implements Converter<String, UserDocumentType> {

    @Override
    @Nullable
    public UserDocumentType convert(@NonNull final String source) {
        final String trimmed = source.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        for (final UserDocumentType type : UserDocumentType.values()) {
            if (type.name().equalsIgnoreCase(trimmed)) {
                return type;
            }
        }
        return null;
    }
}
