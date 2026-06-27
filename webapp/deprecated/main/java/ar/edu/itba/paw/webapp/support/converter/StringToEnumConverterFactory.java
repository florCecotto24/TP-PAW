package ar.edu.itba.paw.webapp.support.converter;

import java.util.Locale;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Generic case-insensitive {@code String → Enum} converter factory wired in
 * {@link ar.edu.itba.paw.webapp.config.WebConfig#addFormatters}. Replaces the per-enum
 * {@code StringTo*Converter} classes that all duplicated the same {@code valueOf(token.trim()
 * .toUpperCase(Locale.ROOT))} body.
 *
 * Behaviour:
 * - Blank input → {@code null} (treated as "not provided" by Spring's collection binder).
 * - Valid token (any casing) → matching enum constant.
 * - Unknown token → {@link IllegalArgumentException} from {@link Enum#valueOf}, which Spring MVC surfaces as {@code MethodArgumentTypeMismatchException} → HTTP 400. This is the explicit fail-fast that replaced the silent {@code collect*Params} drops in the search and reservation-query services.
 * Per-enum custom logic (aliases, legacy DB tokens, etc.) should override the factory by registering a dedicated {@link Converter} for that specific enum; Spring picks the most specific converter for a given target type, so the factory keeps covering the rest.
 */
public final class StringToEnumConverterFactory implements ConverterFactory<String, Enum<?>> {

    @Override
    @NonNull
    public <T extends Enum<?>> Converter<String, T> getConverter(@NonNull final Class<T> targetType) {
        return new StringToEnum<>(targetType);
    }

    private static final class StringToEnum<T extends Enum<?>> implements Converter<String, T> {

        private final Class<T> enumType;

        StringToEnum(final Class<T> enumType) {
            this.enumType = enumType;
        }

        @Override
        @Nullable
        @SuppressWarnings({"unchecked", "rawtypes"})
        public T convert(@NonNull final String source) {
            if (source.isBlank()) {
                return null;
            }
            // The double cast goes through raw Enum because Enum.valueOf is parameterised on
            // <T extends Enum<T>>, but our T's bound is Enum<?>. The runtime check is still
            // sound: targetType is the exact enum class and Enum.valueOf throws IAE for
            // unknown names.
            return (T) Enum.valueOf((Class<? extends Enum>) enumType,
                    source.trim().toUpperCase(Locale.ROOT));
        }
    }
}
