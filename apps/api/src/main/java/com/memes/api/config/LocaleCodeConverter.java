package com.memes.api.config;

import com.memes.api.generated.model.LocaleCode;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Converts {@code ?locale=es} or {@code ?locale=es-ar} query strings into {@link LocaleCode} values.
 * <p>
 * Spring's default enum converter resolves by Enum.name(), so {@code "en"} is
 * not recognised — only {@code "EN"} would be. The OpenAPI generator emits
 * the enum with lowercase string values, so we register an explicit converter
 * that delegates to {@link LocaleCode#fromValue}.
 * <p>
 * Resolution order: exact match on the full normalised tag (e.g. {@code "es-ar"}),
 * then language-only fallback (e.g. {@code "es-MX"} → {@code "es"}).
 */
@Component
public class LocaleCodeConverter implements Converter<String, LocaleCode> {

    @Override
    @Nullable
    public LocaleCode convert(@Nullable String source) {
        if (source == null || source.isBlank()) return null;
        String normalized = source.toLowerCase().replace('_', '-');
        return Optional.ofNullable(tryParse(normalized))
            .orElseGet(() -> tryParse(normalized.split("-")[0]));
    }

    @Nullable
    private static LocaleCode tryParse(String value) {
        try {
            return LocaleCode.fromValue(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
