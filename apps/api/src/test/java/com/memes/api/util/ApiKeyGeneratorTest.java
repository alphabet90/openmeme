package com.memes.api.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ApiKeyGenerator}.
 * Validates key format, randomness, and prefix conventions.
 */
class ApiKeyGeneratorTest {

    private static final Pattern KEY_PATTERN = Pattern.compile("^sk-[A-Za-z0-9_-]+$");

    @Test
    void generate_startsWithPrefix() {
        String key = ApiKeyGenerator.generate();
        assertThat(key).startsWith("sk-");
    }

    @Test
    void generate_matchesExpectedPattern() {
        String key = ApiKeyGenerator.generate();
        assertThat(key).matches(KEY_PATTERN);
    }

    @RepeatedTest(10)
    void generate_producesUniqueKeys() {
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            keys.add(ApiKeyGenerator.generate());
        }
        assertThat(keys).hasSize(100);
    }

    @Test
    void generate_producesReasonableLength() {
        String key = ApiKeyGenerator.generate();
        // prefix (3) + separator (1) + base64url(32 bytes) ~ 43 chars = ~47 total
        assertThat(key.length()).isGreaterThan(40).isLessThan(60);
    }
}
