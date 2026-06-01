package com.memes.api.common.security;

import com.memes.api.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class ApiKeyRateLimiter {

    private final ProxyManager<String> proxyManager;
    private final RateLimitProperties properties;

    private final Map<String, Supplier<BucketConfiguration>> configs = new ConcurrentHashMap<>();

    public boolean isAllowed(long keyId, String role) {
        String key = "rate:" + keyId + ":" + role;
        Supplier<BucketConfiguration> configSupplier = configs.computeIfAbsent(role, this::createConfig);
        var bucket = proxyManager.getProxy(key, configSupplier);
        return bucket != null && bucket.tryConsume(1);
    }

    private Supplier<BucketConfiguration> createConfig(String role) {
        int permitsPerMinute = switch (role.toUpperCase()) {
            case "ADMIN" -> properties.getAdminPerMinute();
            case "WRITE" -> properties.getWritePerMinute();
            default -> properties.getReadPerMinute();
        };
        Bandwidth limit = Bandwidth.classic(permitsPerMinute, Refill.greedy(permitsPerMinute, Duration.ofMinutes(1)));
        return () -> BucketConfiguration.builder().addLimit(limit).build();
    }
}
