package com.memes.api.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.memes.api.generated.model.LocaleCode;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LocaleCodeConverter}.
 * Validates the conversion matrix for locale query parameters.
 */
class LocaleCodeConverterTest {

    private final LocaleCodeConverter converter = new LocaleCodeConverter();

    @ParameterizedTest
    @CsvSource({
        "en, EN",
        "es, ES",
        "pt, PT",
        "fr, FR",
        "de, DE",
        "ar, AR",
        "es-ar, ES_AR",
        "es_AR, ES_AR",
        "ES-AR, ES_AR"
    })
    void convert_validLocales_returnsExpectedCode(String input, LocaleCode expected) {
        LocaleCode result = converter.convert(input);
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void convert_nullOrBlank_returnsNull(String input) {
        LocaleCode result = converter.convert(input);
        assertThat(result).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "es-MX, es",
        "es-AR, es-ar",
        "pt-BR, pt",
        "fr-CA, fr",
        "de-AT, de"
    })
    void convert_regionalVariant_fallsBackToLanguageCode(String input, String expectedValue) {
        LocaleCode result = converter.convert(input);
        Optional<LocaleCode> expected = Optional.ofNullable(LocaleCode.fromValue(expectedValue));
        assertThat(Optional.ofNullable(result)).isEqualTo(expected);
    }

    @Test
    void convert_unknownLanguage_returnsNull() {
        LocaleCode result = converter.convert("xx");
        assertThat(result).isNull();
    }

    @Test
    void convert_unknownRegionalVariant_fallsBackToUnknownLanguage_returnsNull() {
        LocaleCode result = converter.convert("xx-YY");
        assertThat(result).isNull();
    }
}
