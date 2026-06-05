package com.memes.api.util;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ApiKeyHasher}.
 * Validates deterministic SHA-256 hashing behavior.
 */
class ApiKeyHasherTest {

    @Test
    void hash_sameInput_producesSameOutput() {
        String input = "my-secret-key";
        String hash1 = ApiKeyHasher.hash(input);
        String hash2 = ApiKeyHasher.hash(input);
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void hash_differentInputs_produceDifferentOutputs() {
        String hash1 = ApiKeyHasher.hash("key-one");
        String hash2 = ApiKeyHasher.hash("key-two");
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void hash_produces64CharacterHexString() {
        String hash = ApiKeyHasher.hash("any-input");
        assertThat(hash).hasSize(64);
        assertThat(hash).matches(Pattern.compile("^[0-9a-f]+$"));
    }

    @Test
    void hash_emptyString_producesValidHash() {
        String hash = ApiKeyHasher.hash("");
        assertThat(hash).hasSize(64);
        assertThat(hash).isNotBlank();
    }

    @Test
    void hash_unicodeInput_producesValidHash() {
        String hash = ApiKeyHasher.hash("clé-secrète-日本語");
        assertThat(hash).hasSize(64);
        assertThat(hash).matches(Pattern.compile("^[0-9a-f]+$"));
    }
}
