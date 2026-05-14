package com.memes.api.util;

import lombok.experimental.UtilityClass;
import java.security.SecureRandom;
import java.util.Base64;

@UtilityClass
public class ApiKeyGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final String PREFIX = "sk-";

    public String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return PREFIX + ENCODER.encodeToString(bytes);
    }
}
